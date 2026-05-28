#!/usr/bin/env python3


import os
import json
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader
import numpy as np
from typing import Dict, List, Tuple, Optional
import math
from pathlib import Path
from tqdm import tqdm
import random





class KeyboardGrid:
    
    
    def __init__(self, grid_path: str = "configs/keyboard_grid.json"):
        with open(grid_path, 'r') as f:
            self.grids = json.load(f)
        

        self.qwerty = self.grids.get('qwerty_english', self.grids)
        

        self.key_positions = {}
        for key_info in self.qwerty['keys']:
            label = key_info['label']
            hitbox = key_info['hitbox']

            cx = hitbox['x'] + hitbox['w'] / 2
            cy = hitbox['y'] + hitbox['h'] / 2
            self.key_positions[label] = (cx, cy)
        

        self.key_positions['<unk>'] = (180, 107)
        self.key_positions['<pad>'] = (0, 0)
        
        self.width = self.qwerty.get('width', 360)
        self.height = self.qwerty.get('height', 215)
        

        self._key_labels = []
        self._key_coords_np = []
        for label, (kx, ky) in self.key_positions.items():
            if label in ['<unk>', '<pad>']:
                continue
            self._key_labels.append(label)
            self._key_coords_np.append([kx, ky])
        self._key_coords_np = np.array(self._key_coords_np)

    def get_nearest_keys_vectorized(self, xs: np.ndarray, ys: np.ndarray) -> List[str]:
        

        px = xs * self.width
        py = ys * self.height
        points = np.stack([px, py], axis=1)
        

        dists = np.sum((points[:, None, :] - self._key_coords_np[None, :, :])**2, axis=2)
        nearest_indices = np.argmin(dists, axis=1)
        return [self._key_labels[i] for i in nearest_indices]

    def get_nearest_key(self, x: float, y: float) -> str:
        
        min_dist = float('inf')
        nearest = '<unk>'
        for label, (kx, ky) in self.key_positions.items():
            if label in ['<unk>', '<pad>']:
                continue
            dist = ((x - kx) ** 2 + (y - ky) ** 2) ** 0.5
            if dist < min_dist:
                min_dist = dist
                nearest = label
        return nearest


class CharTokenizer:
    
    
    def __init__(self):


        chars = list('abcdefghijklmnopqrstuvwxyz')
        special = ['<pad>', '<unk>', '<sos>', '<eos>']
        
        self.vocab = special + chars
        self.char_to_idx = {c: i for i, c in enumerate(self.vocab)}
        self.idx_to_char = {i: c for c, i in self.char_to_idx.items()}
        
        self.pad_idx = self.char_to_idx['<pad>']
        self.unk_idx = self.char_to_idx['<unk>']
        self.sos_idx = self.char_to_idx['<sos>']
        self.eos_idx = self.char_to_idx['<eos>']
        
        self.vocab_size = len(self.vocab)
    
    def encode_word(self, word: str) -> List[int]:
        
        indices = [self.sos_idx]
        for char in str(word).lower():
            if char in self.char_to_idx:
                indices.append(self.char_to_idx[char])
            else:
                indices.append(self.unk_idx)
        indices.append(self.eos_idx)
        return indices
    
    def decode(self, indices: List[int]) -> str:
        
        chars = []
        for idx in indices:
            if isinstance(idx, torch.Tensor):
                idx = idx.item()
            if idx == self.sos_idx:
                continue
            if idx == self.eos_idx or idx == self.pad_idx:
                break
            chars.append(self.idx_to_char.get(idx, '?'))
        return ''.join(chars)






class SwipeDataset(Dataset):
    
    
    def __init__(self, data_path: str, max_seq_len: int = 150, max_word_len: int = 20):
        self.max_seq_len = max_seq_len
        self.max_word_len = max_word_len
        self.keyboard = KeyboardGrid()
        self.tokenizer = CharTokenizer()
        self.data_path = data_path
        

        self.offsets = []
        self.f_handle = None
        
        if not os.path.exists(data_path):
            print(f"⚠️ Warning: {data_path} not found.")
            return

        print(f"🔍 Indexing {data_path} for Lazy Loading...")
        with open(data_path, 'r') as f:
            while True:
                offset = f.tell()
                line = f.readline()
                if not line:
                    break
                self.offsets.append(offset)
        print(f"✅ Found {len(self.offsets)} samples.")

    def __len__(self):
        return len(self.offsets)

    def __getitem__(self, idx):
        if self.f_handle is None:
            self.f_handle = open(self.data_path, 'r')
        
        self.f_handle.seek(self.offsets[idx])
        line = self.f_handle.readline()
        item = json.loads(line)
        

        curve = {}
        if 'curve' in item:
            curve = item['curve']
        elif 'word_seq' in item:
            curve = item['word_seq']
            if 'time' in curve:
                curve['t'] = curve.pop('time')
        

        xs = np.array(curve['x'], dtype=np.float32)
        ys = np.array(curve['y'], dtype=np.float32)
        ts = np.array(curve['t'], dtype=np.float32)
        

        norm_xs = xs / self.keyboard.width
        norm_ys = ys / self.keyboard.height
        

        dt = np.diff(ts, prepend=ts[0])
        dt = np.maximum(dt, 1e-6)
        
        vx = np.zeros_like(xs)
        vy = np.zeros_like(ys)
        vx[1:] = np.diff(norm_xs) / dt[1:]
        vy[1:] = np.diff(norm_ys) / dt[1:]
        
        ax = np.zeros_like(xs)
        ay = np.zeros_like(ys)
        ax[1:] = np.diff(vx) / dt[1:]
        ay[1:] = np.diff(vy) / dt[1:]
        

        vx = np.clip(vx, -10, 10)
        vy = np.clip(vy, -10, 10)
        ax = np.clip(ax, -10, 10)
        ay = np.clip(ay, -10, 10)
        

        near_keys = self.keyboard.get_nearest_keys_vectorized(norm_xs, norm_ys)
        nk_indices = [self.tokenizer.char_to_idx.get(k, self.tokenizer.unk_idx) for k in near_keys]
        

        traj_features = np.stack([norm_xs, norm_ys, vx, vy, ax, ay], axis=1)
        

        seq_len = min(len(xs), self.max_seq_len)
        traj_features = traj_features[:seq_len]
        nk_indices = nk_indices[:seq_len]
        
        if seq_len < self.max_seq_len:
            pad_len = self.max_seq_len - seq_len
            traj_features = np.pad(traj_features, ((0, pad_len), (0, 0)), mode='constant')
            nk_indices = nk_indices + [self.tokenizer.pad_idx] * pad_len
        

        word = item.get('word', 'unknown')
        target_indices = self.tokenizer.encode_word(word)
        

        if len(target_indices) > self.max_word_len:
            target_indices = target_indices[:self.max_word_len-1] + [self.tokenizer.eos_idx]
        else:
            pad_len = self.max_word_len - len(target_indices)
            target_indices = target_indices + [self.tokenizer.pad_idx] * pad_len
        
        return {
            'traj_features': torch.tensor(traj_features, dtype=torch.float32),
            'nearest_keys': torch.tensor(nk_indices, dtype=torch.long),
            'target': torch.tensor(target_indices, dtype=torch.long),
            'seq_len': seq_len,
            'word': word
        }






class CharacterLevelSwipeModel(nn.Module):
    
    
    def __init__(self,
                 traj_dim: int = 6,
                 d_model: int = 256,
                 nhead: int = 8,
                 num_encoder_layers: int = 6,
                 num_decoder_layers: int = 4,
                 dim_feedforward: int = 1024,
                 dropout: float = 0.1,
                 vocab_size: int = 30,
                 max_seq_len: int = 150):
        super().__init__()
        
        self.d_model = d_model
        

        self.traj_proj = nn.Linear(traj_dim, d_model // 2)
        self.kb_embedding = nn.Embedding(vocab_size, d_model // 2)
        self.encoder_norm = nn.LayerNorm(d_model)
        

        pe = torch.zeros(max_seq_len, d_model)
        position = torch.arange(0, max_seq_len).unsqueeze(1).float()
        div_term = torch.exp(torch.arange(0, d_model, 2).float() * 
                           -(math.log(10000.0) / d_model))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        self.register_buffer('pe', pe.unsqueeze(0))
        

        encoder_layer = nn.TransformerEncoderLayer(
            d_model=d_model,
            nhead=nhead,
            dim_feedforward=dim_feedforward,
            dropout=dropout,
            batch_first=True
        )
        self.encoder = nn.TransformerEncoder(encoder_layer, num_encoder_layers)
        

        self.char_embedding = nn.Embedding(vocab_size, d_model)
        decoder_layer = nn.TransformerDecoderLayer(
            d_model=d_model,
            nhead=nhead,
            dim_feedforward=dim_feedforward,
            dropout=dropout,
            batch_first=True
        )
        self.decoder = nn.TransformerDecoder(decoder_layer, num_decoder_layers)
        

        self.output_proj = nn.Linear(d_model, vocab_size)
        
        self._init_weights()
    
    def _init_weights(self):
        for p in self.parameters():
            if p.dim() > 1:
                nn.init.xavier_uniform_(p)
    
    def encode_trajectory(self, traj_features, nearest_keys, src_mask=None):
        

        traj_enc = self.traj_proj(traj_features)
        kb_enc = self.kb_embedding(nearest_keys)
        

        combined = torch.cat([traj_enc, kb_enc], dim=-1)
        combined = self.encoder_norm(combined)
        

        combined = combined + self.pe[:, :traj_features.shape[1], :]
        

        memory = self.encoder(combined, src_key_padding_mask=src_mask)
        
        return memory
    
    def forward(self, traj_features, nearest_keys, targets, src_mask=None, tgt_mask=None):
        

        memory = self.encode_trajectory(traj_features, nearest_keys, src_mask)
        

        tgt_input = targets[:, :-1]  
        

        tgt_emb = self.char_embedding(tgt_input) * math.sqrt(self.d_model)
        tgt_emb = tgt_emb + self.pe[:, :tgt_input.shape[1], :]
        

        causal_mask = nn.Transformer.generate_square_subsequent_mask(tgt_input.shape[1]).to(traj_features.device)
        

        output = self.decoder(
            tgt_emb, memory,
            tgt_mask=causal_mask,
            memory_key_padding_mask=src_mask,
            tgt_key_padding_mask=tgt_mask
        )
        

        logits = self.output_proj(output)
        
        return logits

    @torch.no_grad()
    def generate_beam(self, traj, nk, tokenizer, src_mask=None, beam_size=5, max_len=20):
        
        self.eval()
        device = traj.device
        batch_size = traj.shape[0]
        memory = self.encode_trajectory(traj, nk, src_mask)
        
        beam_seqs = torch.full((batch_size, beam_size, 1), tokenizer.sos_idx, dtype=torch.long, device=device)
        beam_scores = torch.zeros((batch_size, beam_size), device=device)
        beam_scores[:, 1:] = -1e9 
        finished = torch.zeros((batch_size, beam_size), dtype=torch.bool, device=device)

        for step in range(max_len):
            if finished.all(): break
            

            flat_seqs = beam_seqs.view(-1, step + 1)
            flat_memory = memory.repeat_interleave(beam_size, dim=0)
            flat_src_mask = src_mask.repeat_interleave(beam_size, dim=0) if src_mask is not None else None
            
            tgt_emb = self.char_embedding(flat_seqs) * math.sqrt(self.d_model) + self.pe[:, :step+1, :]
            causal = nn.Transformer.generate_square_subsequent_mask(step + 1).to(device).bool()
            
            out = self.decoder(tgt_emb, flat_memory, tgt_mask=causal, memory_key_padding_mask=flat_src_mask)
            logits = self.output_proj(out[:, -1, :]) 
            
            log_probs = F.log_softmax(logits, dim=-1)
            candidate_scores = beam_scores.unsqueeze(-1).expand(-1, -1, tokenizer.vocab_size).clone()
            candidate_scores += log_probs.view(batch_size, beam_size, -1)
            

            for b in range(batch_size):
                for k in range(beam_size):
                    if finished[b, k]:
                        candidate_scores[b, k, :] = -1e9
                        candidate_scores[b, k, tokenizer.pad_idx] = beam_scores[b, k]
            
            top_scores, top_indices = torch.topk(candidate_scores.view(batch_size, -1), beam_size, dim=-1)
            
            new_seqs = torch.zeros((batch_size, beam_size, step + 2), dtype=torch.long, device=device)
            new_finished = torch.zeros((batch_size, beam_size), dtype=torch.bool, device=device)
            new_scores = torch.zeros((batch_size, beam_size), device=device)
            
            for b in range(batch_size):
                picked_beam_indices = top_indices[b] // tokenizer.vocab_size
                new_tokens = top_indices[b] % tokenizer.vocab_size
                for k in range(beam_size):
                    prev_idx = picked_beam_indices[k]
                    token = new_tokens[k]
                    new_seqs[b, k] = torch.cat([beam_seqs[b, prev_idx], token.unsqueeze(0)])
                    new_scores[b, k] = top_scores[b, k]
                    new_finished[b, k] = finished[b, prev_idx] or (token == tokenizer.eos_idx) or (token == tokenizer.pad_idx)
            
            beam_seqs, beam_scores, finished = new_seqs, new_scores, new_finished

        results = []
        for b in range(batch_size):
            best_beam_idx = torch.argmax(beam_scores[b])
            results.append(tokenizer.decode(beam_seqs[b, best_beam_idx]))
        return results






def worker_init_fn(worker_id):
    worker_info = torch.utils.data.get_worker_info()
    dataset = worker_info.dataset
    if dataset.f_handle is not None:
        dataset.f_handle.close()
    dataset.f_handle = open(dataset.data_path, 'r')

def train():
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print("="*60)
    print("Training Swipe Typing Transformer (IndicSwipe Logic)")
    print("="*60)
    print(f"Device: {device}")
    

    train_path = 'swipes/swipe_train.jsonl'
    val_path = 'swipes/swipe_valid.jsonl'
    checkpoint_dir = Path('checkpoints/full_character_model')
    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    

    train_ds = SwipeDataset(train_path)
    val_ds = SwipeDataset(val_path)
    
    loader_args = {
        'batch_size': 1024 if torch.cuda.is_available() else 32, 
        'num_workers': 16, 
        'pin_memory': True, 
        'worker_init_fn': worker_init_fn
    }

    if not torch.cuda.is_available():
        loader_args = {'batch_size': 32, 'num_workers': 0}
        
    train_loader = DataLoader(train_ds, shuffle=True, **loader_args)
    val_loader = DataLoader(val_ds, **loader_args)
    
    tokenizer = CharTokenizer()
    model = CharacterLevelSwipeModel(vocab_size=tokenizer.vocab_size).to(device)
    

    num_epochs = 50
    learning_rate = 5e-4
    weight_decay = 0.01
    warmup_epochs = 2
    patience = 15
    
    optimizer = torch.optim.AdamW(model.parameters(), lr=learning_rate, weight_decay=weight_decay)
    criterion = nn.CrossEntropyLoss(ignore_index=tokenizer.pad_idx)

    scheduler = torch.optim.lr_scheduler.OneCycleLR(
        optimizer,
        max_lr=learning_rate,
        epochs=num_epochs,
        steps_per_epoch=len(train_loader),
        pct_start=warmup_epochs/num_epochs,
        anneal_strategy='cos'
    )
    

    scaler = torch.amp.GradScaler('cuda')
    
    best_val_acc = 0
    patience_counter = 0
    
    for epoch in range(num_epochs):
        model.train()
        train_loss = 0
        pbar = tqdm(train_loader, desc=f"Epoch {epoch+1}/{num_epochs} [Train]")
        
        for batch in pbar:
            traj = batch['traj_features'].to(device)
            nk = batch['nearest_keys'].to(device)
            tgt = batch['target'].to(device)
            

            seq_lens = batch['seq_len'].to(device)
            src_mask = torch.arange(traj.shape[1], device=device)[None, :] >= seq_lens[:, None]
            tgt_mask = (tgt[:, :-1] == tokenizer.pad_idx)
            
            with torch.amp.autocast('cuda'):
                logits = model(traj, nk, tgt, src_mask, tgt_mask)

                loss = criterion(logits.reshape(-1, logits.shape[-1]), tgt[:, 1:].reshape(-1))
            
            optimizer.zero_grad()
            scaler.scale(loss).backward()
            scaler.unscale_(optimizer)
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            scaler.step(optimizer)
            scaler.update()
            scheduler.step()
            
            train_loss += loss.item()
            pbar.set_postfix({'loss': f"{loss.item():.4f}", 'lr': f"{scheduler.get_last_lr()[0]:.2e}"})
        

        model.eval()
        correct, total_val = 0, 0
        samples = []
        
        print(f"📢 Validating Epoch {epoch+1}...")
        with torch.no_grad():
            for batch in tqdm(val_loader, desc="[Val] Beam Search"):
                traj = batch['traj_features'].to(device)
                nk = batch['nearest_keys'].to(device)
                seq_lens = batch['seq_len'].to(device)
                src_mask = torch.arange(traj.shape[1], device=device)[None, :] >= seq_lens[:, None]
                

                preds = model.generate_beam(traj, nk, tokenizer, src_mask=src_mask)
                
                for p, w in zip(preds, batch['word']):
                    total_val += 1
                    if p.lower() == w.lower():
                        correct += 1
                    if len(samples) < 5 and random.random() < 0.1:
                        samples.append((w, p))
        
        val_acc = correct / total_val
        avg_train_loss = train_loss / len(train_loader)
        
        print("\n--- Sample Predictions ---")
        for target, pred in samples:
            match = "✅" if target.lower() == pred.lower() else "❌"
            print(f"{match} Target: {target:<15} | Pred: {pred}")
        
        print(f"\n📊 Epoch {epoch+1} Summary:")
        print(f"  Train Loss: {avg_train_loss:.4f}")
        print(f"  Val Accuracy: {val_acc:.2%}")
        
        if val_acc > best_val_acc:
            best_val_acc = val_acc
            patience_counter = 0
            

            checkpoint = {
                'epoch': epoch + 1,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'val_word_acc': val_acc,
                'train_loss': avg_train_loss
            }
            checkpoint_path = checkpoint_dir / f'full-model-{epoch+1:02d}-{val_acc:.3f}.pt'
            torch.save(checkpoint, checkpoint_path)
            

            torch.save(checkpoint, checkpoint_dir.parent / 'best_model.pt')
            
            print(f"🌟 New best accuracy! Saved to {checkpoint_path}")
        else:
            patience_counter += 1
            if patience_counter >= patience:
                print(f"🛑 Early stopping triggered after {patience} epochs without improvement.")
                break
        
        print("-" * 60)

if __name__ == "__main__":
    train()