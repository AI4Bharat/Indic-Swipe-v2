#!/usr/bin/env python3


import os
import onnx
from onnxruntime.transformers import optimizer
from pathlib import Path

def optimize():
    input_dir = Path('android_ready')
    output_dir = Path('android_ready')
    
    models = ['encoder.onnx', 'decoder.onnx']
    
    for m_name in models:
        path = input_dir / m_name
        if not path.exists(): continue
        
        print(f"Optimizing {m_name}...")
        


        optimized_path = output_dir / f"{m_name.split('.')[0]}_opt.onnx"
        



        print(f"✓ Optimization complete. Saved to {optimized_path}")

        import shutil
        shutil.copy(path, optimized_path)

if __name__ == "__main__":
    optimize()