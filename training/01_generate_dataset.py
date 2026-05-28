#!/usr/bin/env python3


import os
import json
import numpy as np
import random
import argparse
import re
from pathlib import Path
from tqdm import tqdm
from multiprocessing import Pool, cpu_count

class SwipeSynthesizer:
    def __init__(self, grid_path, width=360, height=215):
        self.width = width
        self.height = height
        self.key_positions = self._load_grid(grid_path)
        
    def _load_grid(self, grid_path):
        with open(grid_path, 'r') as f:
            data = json.load(f)
        

        grid = data.get('qwerty_english', data)
        positions = {}
        for key in grid['keys']:
            label = key['label']
            h = key['hitbox']

            positions[label] = (h['x'] + h['w']/2, h['y'] + h['h']/2)
        return positions

    def dedupe_word(self, word):
        
        if not word: return ""
        result = [word[0]]
        for i in range(1, len(word)):
            if word[i] != word[i-1]:
                result.append(word[i])
        return "".join(result)

    def _catmull_rom_one_point(self, t, p0, p1, p2, p3):
        
        return 0.5 * (
            (2 * p1) +
            (-p0 + p2) * t +
            (2 * p0 - 5 * p1 + 4 * p2 - p3) * (t**2) +
            (-p0 + 3 * p1 - 3 * p2 + p3) * (t**3)
        )

    def generate_trace(self, word, jitter=2.0, sampling_rate=0.03):
        
        word = word.lower()
        deduped = self.dedupe_word(word)
        
        waypoints = []
        for char in deduped:
            if char in self.key_positions:
                cx, cy = self.key_positions[char]
                jx = random.gauss(0, jitter)
                jy = random.gauss(0, jitter)
                waypoints.append(np.array([cx + jx, cy + jy]))
        
        if len(waypoints) < 1:
            return None
        if len(waypoints) == 1:
            w = waypoints[0]
            return {'x': [float(w[0])], 'y': [float(w[1])], 't': [0]}

        p_seq = [waypoints[0]] + waypoints + [waypoints[-1]]
        x_final, y_final, t_final = [], [], []
        current_time = 0
        
        for i in range(1, len(p_seq) - 2):
            p0, p1, p2, p3 = p_seq[i-1], p_seq[i], p_seq[i+1], p_seq[i+2]
            dist = np.linalg.norm(p2 - p1)
            num_steps = max(3, int(dist / 15))
            
            for step in range(num_steps):
                t_step = step / num_steps
                pos = self._catmull_rom_one_point(t_step, p0, p1, p2, p3)
                x_final.append(float(pos[0]))
                y_final.append(float(pos[1]))
                t_final.append(int(current_time * 1000))
                current_time += sampling_rate
                
        x_final.append(float(waypoints[-1][0]))
        y_final.append(float(waypoints[-1][1]))
        t_final.append(int(current_time * 1000))
        
        return {'x': x_final, 'y': y_final, 't': t_final}


synthesizer = None
jitter_val = 2.5

def init_worker(grid_path, jitter):
    global synthesizer, jitter_val
    synthesizer = SwipeSynthesizer(grid_path)
    jitter_val = jitter

def process_line(line):
    try:
        item = json.loads(line)
        word = item.get('english word', item.get('word', ''))
        if not word: return None
        

        if not re.match(r'^[a-z]+$', word.lower()):
            return None
            
        trace = synthesizer.generate_trace(word, jitter=jitter_val)
        if trace:
            return json.dumps({"curve": trace, "word": word, "grid_name": "qwerty_english"})
    except Exception:
        return None
    return None

def main():
    parser = argparse.ArgumentParser(description="Step 1: Generate synthetic swipe dataset (Multiprocessed)")
    parser.add_argument("--input", type=str, required=True, help="Path to input JSONL file")
    parser.add_argument("--output", type=str, required=True, help="Output JSONL file")
    parser.add_argument("--grid", type=str, default="configs/keyboard_grid.json", help="Keyboard grid config")
    parser.add_argument("--jitter", type=float, default=2.5, help="Pixel jitter std-dev")
    parser.add_argument("--workers", type=int, default=cpu_count(), help="Number of workers")
    
    args = parser.parse_args()
    if not os.path.exists(args.grid):
        print(f"Error: Grid file not found at {args.grid}")
        return


    print(f"Counting lines in {args.input}...")
    with open(args.input, 'r') as f:
        total_lines = sum(1 for _ in f)

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    
    print(f"Starting generation with {args.workers} workers...")
    with open(args.input, 'r') as f, open(args.output, 'w') as out_f, Pool(args.workers, initializer=init_worker, initargs=(args.grid, args.jitter)) as pool:
        for result in tqdm(pool.imap_unordered(process_line, f, chunksize=100), total=total_lines):
            if result:
                out_f.write(result + "\n")
                
    print(f"✅ Finished generating swipes in {args.output}")

if __name__ == "__main__":
    main()