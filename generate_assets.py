import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def generate_base_logo():
    size = (512, 512)
    img = Image.new("RGBA", size, (0, 0, 0, 0))

    # Dark Rounded Square Card
    card_margin = 16
    card_bounds = [card_margin, card_margin, size[0] - card_margin, size[1] - card_margin]
    corner_radius = 90

    # Background Layer
    bg = Image.new("RGBA", size, (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg)
    bg_draw.rounded_rectangle(card_bounds, radius=corner_radius, fill=(12, 16, 26, 255))

    # Outer Neon Glow Ring
    glow_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_bounds = [card_margin - 4, card_margin - 4, size[0] - card_margin + 4, size[1] - card_margin + 4]
    glow_draw.rounded_rectangle(glow_bounds, radius=corner_radius + 4, outline=(0, 229, 255, 200), width=10)
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(10))

    # Inner Purple Border
    border_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    border_draw = ImageDraw.Draw(border_layer)
    border_draw.rounded_rectangle(card_bounds, radius=corner_radius, outline=(157, 78, 221, 240), width=6)

    img = Image.alpha_composite(img, glow_layer)
    img = Image.alpha_composite(img, bg)
    img = Image.alpha_composite(img, border_layer)
    draw = ImageDraw.Draw(img)

    # TV Screen Frame
    tv_w, tv_h = 260, 175
    tv_left = (size[0] - tv_w) // 2
    tv_top = (size[1] - tv_h) // 2 - 30
    tv_bounds = [tv_left, tv_top, tv_left + tv_w, tv_top + tv_h]

    # TV Glow
    tv_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    tv_glow_draw = ImageDraw.Draw(tv_glow)
    tv_glow_draw.rounded_rectangle(tv_bounds, radius=24, fill=(0, 229, 255, 70))
    tv_glow = tv_glow.filter(ImageFilter.GaussianBlur(16))
    img = Image.alpha_composite(img, tv_glow)
    draw = ImageDraw.Draw(img)

    # TV Screen
    draw.rounded_rectangle(tv_bounds, radius=24, fill=(22, 27, 42, 255), outline=(0, 229, 255, 255), width=5)

    # TV Stand Legs
    stand_cx = size[0] // 2
    stand_top = tv_top + tv_h
    draw.line([(stand_cx - 45, stand_top + 22), (stand_cx, stand_top + 2)], fill=(0, 229, 255, 255), width=5)
    draw.line([(stand_cx + 45, stand_top + 22), (stand_cx, stand_top + 2)], fill=(0, 229, 255, 255), width=5)

    # Play Button Triangle
    play_cx = size[0] // 2 + 6
    play_cy = tv_top + tv_h // 2
    play_size = 42
    play_pts = [
        (play_cx - play_size + 10, play_cy - play_size + 5),
        (play_cx - play_size + 10, play_cy + play_size - 5),
        (play_cx + play_size - 5, play_cy)
    ]

    play_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    play_glow_draw = ImageDraw.Draw(play_glow)
    play_glow_draw.polygon(play_pts, fill=(255, 0, 128, 220))
    play_glow = play_glow.filter(ImageFilter.GaussianBlur(12))
    img = Image.alpha_composite(img, play_glow)
    draw = ImageDraw.Draw(img)

    draw.polygon(play_pts, fill=(255, 0, 150, 255))

    # Branding Text
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 44)
        font_sub = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 20)
    except:
        font = ImageFont.load_default()
        font_sub = font

    text = "FSTV"
    sub_text = "PLAYER"

    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    tx = (size[0] - tw) // 2
    ty = size[1] - 110

    # Text Glow
    text_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    text_glow_draw = ImageDraw.Draw(text_glow)
    text_glow_draw.text((tx, ty), text, font=font, fill=(0, 229, 255, 255))
    text_glow = text_glow.filter(ImageFilter.GaussianBlur(8))
    img = Image.alpha_composite(img, text_glow)
    draw = ImageDraw.Draw(img)

    draw.text((tx, ty), text, font=font, fill=(255, 255, 255, 255))

    bbox_sub = draw.textbbox((0, 0), sub_text, font=font_sub)
    sw = bbox_sub[2] - bbox_sub[0]
    draw.text(((size[0] - sw) // 2, ty + 48), sub_text, font=font_sub, fill=(157, 78, 221, 255))

    return img

def generate_banner(output_path):
    size = (1280, 720)
    img = Image.new("RGBA", size, (10, 12, 18, 255))
    draw = ImageDraw.Draw(img)

    # Ambient Orbs
    glow1 = Image.new("RGBA", size, (0, 0, 0, 0))
    g1_draw = ImageDraw.Draw(glow1)
    g1_draw.ellipse([100, -100, 700, 500], fill=(0, 229, 255, 45))
    glow1 = glow1.filter(ImageFilter.GaussianBlur(80))

    glow2 = Image.new("RGBA", size, (0, 0, 0, 0))
    g2_draw = ImageDraw.Draw(glow2)
    g2_draw.ellipse([600, 200, 1300, 800], fill=(157, 78, 221, 55))
    glow2 = glow2.filter(ImageFilter.GaussianBlur(100))

    img = Image.alpha_composite(img, glow1)
    img = Image.alpha_composite(img, glow2)
    draw = ImageDraw.Draw(img)

    # Left Typography
    try:
        font_brand = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 64)
        font_tag = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 26)
        font_bullet = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 22)
        font_btn = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    except:
        font_brand = ImageFont.load_default()
        font_tag = font_brand
        font_bullet = font_brand
        font_btn = font_brand

    lx, ly = 100, 200

    b_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(b_glow)
    bg_draw.text((lx, ly), "FSTV PLAYER", font=font_brand, fill=(0, 229, 255, 255))
    b_glow = b_glow.filter(ImageFilter.GaussianBlur(12))
    img = Image.alpha_composite(img, b_glow)
    draw = ImageDraw.Draw(img)

    draw.text((lx, ly), "FSTV PLAYER", font=font_brand, fill=(255, 255, 255, 255))
    draw.text((lx, ly + 80), "A Experiência Definitiva em IPTV para Android TV", font=font_tag, fill=(157, 78, 221, 255))

    bullets = [
        "⚡ Carregamento Instantâneo com Cache Inteligente",
        "🎭 Séries Organizadas por Temporadas e Episódios",
        "📡 Suporte Completo a Canais ao Vivo, Filmes & VOD",
        "📺 Interface Fluida para Controle Remoto"
    ]

    by = ly + 150
    for bullet in bullets:
        draw.text((lx, by), bullet, font=font_bullet, fill=(200, 210, 225, 255))
        by += 45

    # Right Mockup Card
    rx, ry, rw, rh = 720, 160, 480, 400
    draw.rounded_rectangle([rx, ry, rx + rw, ry + rh], radius=24, fill=(18, 22, 34, 230), outline=(0, 229, 255, 200), width=3)
    draw.rounded_rectangle([rx + 20, ry + 20, rx + 140, ry + 60], radius=10, fill=(0, 229, 255, 255))
    draw.text((rx + 35, ry + 30), "🏠 INÍCIO", font=font_btn, fill=(0, 0, 0, 255))

    cats = ["📡 Canais ao Vivo", "🎬 Filmes VOD", "🎭 Séries (Netflix)", "⚽ Esportes HD"]
    cy = ry + 80
    for c in cats:
        is_active = "Séries" in c
        bg_col = (157, 78, 221, 220) if is_active else (28, 34, 50, 180)
        txt_col = (255, 255, 255) if is_active else (160, 170, 190)
        draw.rounded_rectangle([rx + 20, cy, rx + 220, cy + 40], radius=8, fill=bg_col)
        draw.text((rx + 30, cy + 10), c, font=font_btn, fill=txt_col)
        cy += 50

    cx = rx + 240
    cy = ry + 80
    for i in range(3):
        draw.rounded_rectangle([cx, cy, cx + 210, cy + 90], radius=12, fill=(35, 42, 64, 255), outline=(0, 229, 255, 120), width=2)
        draw.text((cx + 15, cy + 30), f"🎭 Série #{i+1}", font=font_btn, fill=(255, 255, 255, 255))
        draw.text((cx + 15, cy + 55), "4 Temp. | 36 Ep.", font=font_btn, fill=(0, 229, 255, 255))
        cy += 100

    img.save(output_path, "PNG")

if __name__ == "__main__":
    base_logo = generate_base_logo()

    res_dir = "/home/felipe/.gemini/antigravity/scratch/Fstv-Player/app/src/main/res"

    # Mipmap resolutions
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
        
        resized = base_logo.resize((dim, dim), Image.Resampling.LANCZOS)
        resized.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")
        resized.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")

    banner_path = os.path.join(res_dir, "drawable", "fstv_banner.png")
    generate_banner(banner_path)

    # Artifact copies
    artifact_dir = "/home/felipe/.gemini/antigravity/brain/413f76fc-fef0-4417-953a-f191a79e0bb6"
    base_logo.save(os.path.join(artifact_dir, "fstv_logo.png"), "PNG")
    generate_banner(os.path.join(artifact_dir, "fstv_banner.png"))

    print("All app icons (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) and banner generated successfully!")
