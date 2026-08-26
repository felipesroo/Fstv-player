import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance

def create_glow(draw_obj, shape_func, glow_color, max_blur=15, steps=5):
    for i in range(steps, 0, -1):
        blur_radius = i * (max_blur // steps)
        # Glow simulated by drawing expanding semi-transparent shapes
        shape_func(draw_obj, i)

def generate_logo(output_path):
    size = (512, 512)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Dark Rounded Square Card
    card_margin = 32
    card_bounds = [card_margin, card_margin, size[0] - card_margin, size[1] - card_margin]
    corner_radius = 80

    # Create background layer with gradient
    bg = Image.new("RGBA", size, (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg)
    
    # Base dark rounded rectangle
    bg_draw.rounded_rectangle(card_bounds, radius=corner_radius, fill=(15, 18, 28, 255))
    
    # Outer Neon Glow Ring
    glow_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_bounds = [card_margin - 6, card_margin - 6, size[0] - card_margin + 6, size[1] - card_margin + 6]
    glow_draw.rounded_rectangle(glow_bounds, radius=corner_radius + 6, outline=(0, 229, 255, 180), width=8)
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(12))

    # Inner Neon Border
    border_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    border_draw = ImageDraw.Draw(border_layer)
    border_draw.rounded_rectangle(card_bounds, radius=corner_radius, outline=(157, 78, 221, 230), width=6)

    # Combine background layers
    img = Image.alpha_composite(img, glow_layer)
    img = Image.alpha_composite(img, bg)
    img = Image.alpha_composite(img, border_layer)
    draw = ImageDraw.Draw(img)

    # 2. Draw TV Screen Frame in Center
    tv_w, tv_h = 240, 160
    tv_left = (size[0] - tv_w) // 2
    tv_top = (size[1] - tv_h) // 2 - 25
    tv_bounds = [tv_left, tv_top, tv_left + tv_w, tv_top + tv_h]

    # TV Screen Glow
    tv_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    tv_glow_draw = ImageDraw.Draw(tv_glow)
    tv_glow_draw.rounded_rectangle(tv_bounds, radius=24, fill=(0, 229, 255, 60))
    tv_glow = tv_glow.filter(ImageFilter.GaussianBlur(15))
    img = Image.alpha_composite(img, tv_glow)
    draw = ImageDraw.Draw(img)

    # TV Screen Body
    draw.rounded_rectangle(tv_bounds, radius=24, fill=(24, 28, 42, 255), outline=(0, 229, 255, 255), width=4)

    # TV Stand / Legs
    stand_cx = size[0] // 2
    stand_top = tv_top + tv_h
    draw.line([(stand_cx - 40, stand_top + 20), (stand_cx, stand_top + 2)], fill=(0, 229, 255, 255), width=4)
    draw.line([(stand_cx + 40, stand_top + 20), (stand_cx, stand_top + 2)], fill=(0, 229, 255, 255), width=4)

    # 3. Glowing Play Triangle Button in Center of TV
    play_cx = size[0] // 2 + 5
    play_cy = tv_top + tv_h // 2
    play_size = 40
    play_pts = [
        (play_cx - play_size + 10, play_cy - play_size + 5),
        (play_cx - play_size + 10, play_cy + play_size - 5),
        (play_cx + play_size - 5, play_cy)
    ]

    play_glow = Image.new("RGBA", size, (0, 0, 0, 0))
    play_glow_draw = ImageDraw.Draw(play_glow)
    play_glow_draw.polygon(play_pts, fill=(255, 0, 128, 200))
    play_glow = play_glow.filter(ImageFilter.GaussianBlur(10))
    img = Image.alpha_composite(img, play_glow)
    draw = ImageDraw.Draw(img)

    draw.polygon(play_pts, fill=(255, 0, 150, 255))

    # 4. Text "FSTV PLAYER"
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 38)
        font_sub = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
    except:
        font = ImageFont.load_default()
        font_sub = font

    text = "FSTV PLAYER"
    sub_text = "ULTRA IPTV"

    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = (size[0] - tw) // 2
    ty = size[1] - 90

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
    draw.text(((size[0] - sw) // 2, ty + th + 10), sub_text, font=font_sub, fill=(157, 78, 221, 255))

    img.save(output_path, "PNG")
    print(f"Generated Logo: {output_path}")

def generate_banner(output_path):
    size = (1280, 720)
    img = Image.new("RGBA", size, (10, 12, 18, 255))
    draw = ImageDraw.Draw(img)

    # Ambient Glow Orbs
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

    # Left Side Typography & Branding
    try:
        font_brand = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 64)
        font_tag = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 26)
        font_bullet = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 22)
    except:
        font_brand = ImageFont.load_default()
        font_tag = font_brand
        font_bullet = font_brand

    lx = 100
    ly = 200

    # Glowing Brand Name
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

    # Right Side Interface Preview Mockup Card
    rx, ry, rw, rh = 720, 160, 480, 400
    draw.rounded_rectangle([rx, ry, rx + rw, ry + rh], radius=24, fill=(18, 22, 34, 230), outline=(0, 229, 255, 200), width=3)

    # Top Mockup Navigation Bar
    draw.rounded_rectangle([rx + 20, ry + 20, rx + 140, ry + 60], radius=10, fill=(0, 229, 255, 255))
    try:
        font_btn = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    except:
        font_btn = ImageFont.load_default()
    draw.text((rx + 35, ry + 30), "🏠 INÍCIO", font=font_btn, fill=(0, 0, 0, 255))

    # Categories Sidebar Mockup
    cats = ["📡 Canais ao Vivo", "🎬 Filmes VOD", "🎭 Séries (Netflix)", "⚽ Esportes HD"]
    cy = ry + 80
    for c in cats:
        is_active = "Séries" in c
        bg_col = (157, 78, 221, 220) if is_active else (28, 34, 50, 180)
        txt_col = (255, 255, 255) if is_active else (160, 170, 190)
        draw.rounded_rectangle([rx + 20, cy, rx + 220, cy + 40], radius=8, fill=bg_col)
        draw.text((rx + 30, cy + 10), c, font=font_btn, fill=txt_col)
        cy += 50

    # Content Cards Mockup
    cx = rx + 240
    cy = ry + 80
    for i in range(3):
        draw.rounded_rectangle([cx, cy, cx + 210, cy + 90], radius=12, fill=(35, 42, 64, 255), outline=(0, 229, 255, 120), width=2)
        draw.text((cx + 15, cy + 30), f"🎭 Série #{i+1}", font=font_btn, fill=(255, 255, 255, 255))
        draw.text((cx + 15, cy + 55), "4 Temp. | 36 Ep.", font=font_btn, fill=(0, 229, 255, 255))
        cy += 100

    img.save(output_path, "PNG")
    print(f"Generated Banner: {output_path}")

if __name__ == "__main__":
    os.makedirs("/home/felipe/.gemini/antigravity/scratch/Fstv-Player/app/src/main/res/mipmap-xxxhdpi", exist_ok=True)
    os.makedirs("/home/felipe/.gemini/antigravity/scratch/Fstv-Player/app/src/main/res/drawable", exist_ok=True)

    logo_path = "/home/felipe/.gemini/antigravity/scratch/Fstv-Player/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
    banner_path = "/home/felipe/.gemini/antigravity/scratch/Fstv-Player/app/src/main/res/drawable/fstv_banner.png"

    generate_logo(logo_path)
    generate_banner(banner_path)

    # Also copy to artifacts directory for display
    artifact_dir = "/home/felipe/.gemini/antigravity/brain/413f76fc-fef0-4417-953a-f191a79e0bb6"
    generate_logo(os.path.join(artifact_dir, "fstv_logo.png"))
    generate_banner(os.path.join(artifact_dir, "fstv_banner.png"))
