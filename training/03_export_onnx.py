#!/usr/bin/env python3


import os
import json
import math
import torch
import torch.nn as nn
import numpy as np
from pathlib import Path
import onnx





class SequenceTokenizer:
    def __init__(self):
        self.char_to_idx = {'<pad>': 0, '<unk>': 1, '<sos>': 2, '<eos>': 3}
        for i, char in enumerate('abcdefghijklmnopqrstuvwxyz'):
            self.char_to_idx[char] = i + 4
        self.idx_to_char = {v: k for k, v in self.char_to_idx.items()}
        self.pad_idx, self.eos_idx, self.sos_idx, self.vocab_size = 0, 3, 2, 30

class IndicSwipeTransformer(nn.Module):
    def __init__(self, traj_dim=6, d_model=256, nhead=8, num_encoder_layers=6, 
                 num_decoder_layers=4, vocab_size=30, max_seq_len=150):
        super().__init__()
        self.d_model = d_model

        self.spatial_proj = nn.Linear(traj_dim, d_model // 2)
        self.layout_embedding = nn.Embedding(vocab_size, d_model // 2)
        self.encoder_norm = nn.LayerNorm(d_model)
        

        pe = torch.zeros(max_seq_len, d_model)
        pos = torch.arange(0, max_seq_len).unsqueeze(1)
        div = torch.exp(torch.arange(0, d_model, 2) * -(math.log(10000.0) / d_model))
        pe[:, 0::2], pe[:, 1::2] = torch.sin(pos * div), torch.cos(pos * div)
        self.register_buffer('pe', pe.unsqueeze(0))
        

        self.encoder = nn.TransformerEncoder(nn.TransformerEncoderLayer(d_model, nhead, 1024, 0.1, batch_first=True), num_encoder_layers)
        self.glyph_embedding = nn.Embedding(vocab_size, d_model)
        self.decoder = nn.TransformerDecoder(nn.TransformerDecoderLayer(d_model, nhead, 1024, 0.1, batch_first=True), num_decoder_layers)
        self.output_proj = nn.Linear(d_model, vocab_size)

    def process_spatial_path(self, spatial_features, key_proximity, src_mask=None):

        traj_enc = self.spatial_proj(spatial_features)
        kb_enc = self.layout_embedding(key_proximity) 
        
        combined = torch.cat([traj_enc, kb_enc], dim=-1)
        

        combined = self.encoder_norm(combined)
        
        combined = combined + self.pe[:, :spatial_features.shape[1], :]
        memory = self.encoder(combined, src_key_padding_mask=src_mask)
        return memory




def export():
    out_dir = Path('android_ready')
    out_dir.mkdir(exist_ok=True)
    ckpt_path = Path('checkpoints/best_model.pt')
    
    if not ckpt_path.exists():
        print(f"❌ Error: {ckpt_path} not found!")
        return

    tokenizer = SequenceTokenizer()
    model = IndicSwipeTransformer(vocab_size=tokenizer.vocab_size)
    
    try:
        ckpt = torch.load(ckpt_path, map_location='cpu', weights_only=False)
        model.load_state_dict(ckpt['model_state_dict'] if 'model_state_dict' in ckpt else ckpt)
        print(f"✅ Loaded checkpoint: {ckpt_path}")
    except Exception as e:
        print(f"❌ Error loading checkpoint: {e}")
        return

    model.eval()


    dyn_axes_enc = {
        'trajectory_features': {0: 'batch', 1: 'sequence'}, 
        'nearest_keys': {0: 'batch', 1: 'sequence'}, 
        'src_mask': {0: 'batch', 1: 'sequence'}, 
        'encoder_output': {0: 'batch', 1: 'sequence'}
    }
    


    dyn_axes_dec = {
        'memory': {0: 'batch', 1: 'enc_sequence'}, 
        'target_tokens': {0: 'batch'}, 
        'src_mask': {0: 'batch', 1: 'enc_sequence'}, 
        'target_mask': {0: 'batch'}, 
        'logits': {0: 'batch'}
    }


    class EncoderWrapper(nn.Module):
        def __init__(self, m): super().__init__(); self.m = m
        def forward(self, spatial_features, key_proximity, src_mask): return self.m.process_spatial_path(spatial_features, key_proximity, src_mask)


    traj_ex, nk_ex, src_m_ex = torch.randn(1, 150, 6), torch.randint(0, 30, (1, 150)), torch.zeros(1, 150, dtype=torch.bool)
    
    print(f"🚀 Exporting Encoder...")
    
    dyn_axes_enc = {
        'trajectory_features': {0: 'batch'}, 
        'nearest_keys': {0: 'batch'}, 
        'src_mask': {0: 'batch'}, 
        'encoder_output': {0: 'batch'}
    }
    
    torch.onnx.export(
        EncoderWrapper(model), (traj_ex, nk_ex, src_m_ex), out_dir / 'encoder.onnx',
        input_names=['trajectory_features', 'nearest_keys', 'src_mask'],
        output_names=['encoder_output'], opset_version=14, do_constant_folding=True,
        dynamic_axes=dyn_axes_enc
    )
    print(f"✅ Encoder Exported to {out_dir}/encoder.onnx")


    class DecoderWrapper(nn.Module):
        def __init__(self, m): super().__init__(); self.m = m
        def forward(self, mem, tgt, src_m, tgt_m):
            t_len = 20
            t_emb = self.m.glyph_embedding(tgt) * math.sqrt(self.m.d_model) + self.m.pe[:, :t_len, :]
            causal = nn.Transformer.generate_square_subsequent_mask(t_len).to(t_emb.device)
            out = self.m.decoder(t_emb, mem, tgt_mask=causal, memory_key_padding_mask=src_m, tgt_key_padding_mask=tgt_m)
            return self.m.output_proj(out)

    mem_ex, tgt_ex, tgt_m_ex = torch.randn(1, 150, 256), torch.zeros(1, 20, dtype=torch.long), torch.zeros(1, 20, dtype=torch.bool)
    
    print(f"🚀 Exporting Decoder...")
    
    dyn_axes_dec = {
        'memory': {0: 'batch'}, 
        'target_tokens': {0: 'batch'}, 
        'src_mask': {0: 'batch'}, 
        'target_mask': {0: 'batch'}, 
        'logits': {0: 'batch'}
    }
    
    torch.onnx.export(
        DecoderWrapper(model), (mem_ex, tgt_ex, src_m_ex, tgt_m_ex), out_dir / 'decoder.onnx',
        input_names=['memory', 'target_tokens', 'src_mask', 'target_mask'],
        output_names=['logits'], opset_version=14, do_constant_folding=True,
        dynamic_axes=dyn_axes_dec
    )
    print(f"✅ Decoder Exported to {out_dir}/decoder.onnx")

if __name__ == "__main__": export()