#!/usr/bin/env python3


from onnxruntime.quantization import quantize_dynamic, QuantType
from pathlib import Path

def quantize():
    dir = Path('android_ready')
    models = ['encoder_opt.onnx', 'decoder_opt.onnx']
    
    for m_name in models:
        path = dir / m_name
        if not path.exists(): continue
        
        output_path = dir / f"{m_name.split('.')[0]}_quant.onnx"
        print(f"Quantizing {m_name}...")
        
        quantize_dynamic(
            model_input=path,
            model_output=output_path,
            weight_type=QuantType.QInt8
        )
        print(f"✓ Quantization complete. Saved to {output_path}")

if __name__ == "__main__":
    quantize()