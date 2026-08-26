import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def generate_minimal_logo():
    size = (512, 512)
    img = Image.new("RGBA", size, (0, 0, 0, 0))

    # Dark Card Background
    card_margin = 16
    card_bounds = [card_margin, card_margin, size[0] - card_margin, size[1] - card_margin]
    corner_radius = 80

    bg = Image.new("RGBA", size, (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg)
    bg_draw.rounded_rectangle(card_bounds, radius=corner_radius, fill=(12, 15, 24, 255))

    # Outer Neon Glow Ring
    glow_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_bounds = [card_margin - 4, card_margin - 4, size[0] - card_margin + 4, size[1] - card_margin + 4]
    glow_draw.rounded_rectangle(glow_bounds, radius=corner_radius + 4, outline=(0, 229, 255, 180), width=8)
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(12))

    # Inner Purple Border
    border_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    border_draw = ImageDraw.Draw(border_layer)
    border_draw.rounded_rectangle(card_bounds, radius=corner_radius, outline=(157, 78, 221, 230), width=6)

    img = Image.alpha_composite(img, glow_layer)
    img = Image.alpha_composite(img, bg)
    img = Image.alpha_composite(img, border_layer)
    draw = ImageDraw.Draw(img)

    # TV Screen Frame (Massive & Centered)
    tv_w, tv_h = 320, 210
    tv_left = (size[0] - tv_w) // 2
    tv_top = (size[1] - tv_h) // 2 - 45
    tv_bounds = [tv_left, tv_top, tv_left + tv_w, tv_top + tv_h]

    # TV Ambient Glow
    tv_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    tv_glow_draw = ImageDraw.Draw(tv_glow)
    tv_glow_draw.rounded_rectangle(tv_bounds, radius=32, fill=(0, 229, 255, 80))
    tv_glow = tv_glow.filter(ImageFilter.GaussianBlur(24))
    img = Image.alpha_composite(img, tv_glow)
    draw = ImageDraw.Draw(img)

    # TV Screen Body
    draw.rounded_rectangle(tv_bounds, radius=32, fill=(20, 26, 42, 255), outline=(0, 229, 255, 255), width=7)

    # TV Stand Legs
    stand_cx = size[0] // 2
    stand_top = tv_top + tv_h
    draw.line([(stand_cx - 55, stand_top + 28), (stand_cx, stand_top + 2)], fill=(0, 229, 255, 255), width=7)
    draw.line([(stand_cx + 55, stand_top + 28), (stand_cx, stand_top + 2)], fill=(0, 229, 255, 255), width=7)

    # Magenta Play Button Triangle inside TV
    play_cx = size[0] // 2 + 8
    play_cy = tv_top + tv_h // 2
    play_size = 52
    play_pts = [
        (play_cx - play_size + 12, play_cy - play_size + 6),
        (play_cx - play_size + 12, play_cy + play_size - 6),
        (play_cx + play_size - 6, play_cy)
    ]

    play_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    play_glow_draw = ImageDraw.Draw(play_glow)
    play_glow_draw.polygon(play_pts, fill=(255, 0, 128, 240))
    play_glow = play_glow.filter(ImageFilter.GaussianBlur(15))
    img = Image.alpha_composite(img, play_glow)
    draw = ImageDraw.Draw(img)

    draw.polygon(play_pts, fill=(255, 0, 150, 255))

    # Bold Clean App Title "FSTV" (Only the name, no extra text)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 64)
    except:
        font = ImageFont.load_default()

    text = "FSTV"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    tx = (size[0] - tw) // 2
    ty = size[1] - 110

    # Text Glow
    text_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    text_glow_draw = ImageDraw.Draw(text_glow)
    text_glow_draw.text((tx, ty), text, font=font, fill=(0, 229, 255, 255))
    text_glow = text_glow.filter(ImageFilter.GaussianBlur(10))
    img = Image.alpha_composite(img, text_glow)
    draw = ImageDraw.Draw(img)

    draw.text((tx, ty), text, font=font, fill=(255, 255, 255, 255))

    return img

if __name__ == "__main__":
    logo_img = generate_minimal_logo()

    res_dir = "/home/felipe/.gemini/antigravity/scratch/Fstv-Player/app/src/main/res"

    mipmaps = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, dim in mipmaps.items():
        target_dir = os.path.join(res_dir, folder)
        os.makedirs(target_dir, exist_ok=True)
        
        resized = logo_img.resize((dim, dim), Image.Resampling.LANCZOS)
        resized.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")
        resized.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")

    artifact_dir = "/home/felipe/.gemini/antigravity/brain/413f76fc-fef0-4417-953a-f191a79e0bb6"
    logo_img.save(os.path.join(artifact_dir, "fstv_logo.png"), "PNG")

    print("Ultra-minimal TV logo generated successfully!")
