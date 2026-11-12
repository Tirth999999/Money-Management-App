import os
import math
from PIL import Image, ImageDraw, ImageFont

os.makedirs("distribution", exist_ok=True)

def draw_jarvis_logo(draw, cx, cy, radius):
    """Draws an aesthetic futuristic Jarvis arc reactor + rupee financial node in Modern Royal Blue"""
    # Royal Blue & Cyan Fintech Accents
    arc_color = (37, 99, 235)      # Royal Blue #2563EB
    sky_color = (96, 165, 250)     # Sky Blue #60A5FA
    cyan_dot = (56, 189, 248)      # Cyan #38BDF8
    white = (255, 255, 255)
    
    # Outer segmented arc ring
    r_outer = radius
    draw.arc([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], start=25, end=65, fill=arc_color, width=max(4, int(radius * 0.045)))
    draw.arc([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], start=115, end=155, fill=arc_color, width=max(4, int(radius * 0.045)))
    draw.arc([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], start=205, end=245, fill=arc_color, width=max(4, int(radius * 0.045)))
    draw.arc([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], start=295, end=335, fill=arc_color, width=max(4, int(radius * 0.045)))
    
    # 4 Cardinal tech node dots (Cyan Azure)
    dot_dist = r_outer * 1.08
    dot_r = max(4, int(radius * 0.05))
    nodes = [(cx, cy - dot_dist), (cx, cy + dot_dist), (cx - dot_dist, cy), (cx + dot_dist, cy)]
    for nx, ny in nodes:
        draw.ellipse([nx - dot_r, ny - dot_r, nx + dot_r, ny + dot_r], fill=cyan_dot)
        
    # Inner reactor ring
    r_inner = radius * 0.76
    draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], outline=sky_color, width=max(3, int(radius * 0.035)))
    
    # Subtle dark navy core fill
    r_core = radius * 0.65
    draw.ellipse([cx - r_core, cy - r_core, cx + r_core, cy + r_core], fill=(15, 23, 42))

    # Futuristic sharp ₹ (Rupee) symbol centered in reactor
    scale = radius * 0.48
    top_y = cy - scale * 0.72
    h_bar = max(4, int(scale * 0.13))
    w_bar = scale * 0.95
    left_x = cx - scale * 0.42

    # Top horizontal bar
    draw.rectangle([left_x, top_y, left_x + w_bar, top_y + h_bar], fill=white)
    
    # Second horizontal bar
    bar2_y = top_y + scale * 0.32
    draw.rectangle([left_x, bar2_y, left_x + w_bar * 0.85, bar2_y + h_bar], fill=white)

    # Curved upper belly of ₹
    arc_box = [left_x - scale * 0.1, bar2_y, left_x + scale * 0.95, bar2_y + scale * 0.75]
    draw.arc(arc_box, start=270, end=90, fill=white, width=h_bar)
    
    # Stem of ₹
    draw.rectangle([left_x, top_y, left_x + h_bar, bar2_y + scale * 0.75], fill=white)
    
    # Downward diagonal leg of ₹
    leg_start_x = left_x + scale * 0.25
    leg_start_y = bar2_y + scale * 0.65
    leg_end_x = left_x + scale * 0.95
    leg_end_y = top_y + scale * 1.5
    draw.line([leg_start_x, leg_start_y, leg_end_x, leg_end_y], fill=white, width=h_bar)


# 1. GENERATE APP ICON (512 x 512)
def generate_icon_512():
    img = Image.new("RGBA", (512, 512), (11, 17, 32, 255)) # Midnight Navy
    draw = ImageDraw.Draw(img)
    
    # Draw background subtle concentric rings
    for r in [230, 180, 120]:
        draw.ellipse([256 - r, 256 - r, 256 + r, 256 + r], outline=(30, 41, 59, 255), width=2)
        
    draw_jarvis_logo(draw, 256, 256, 175)
    img.save("distribution/icon_512.png", "PNG")
    print("Generated distribution/icon_512.png (512x512)")

# 2. GENERATE FEATURE GRAPHIC (1024 x 500)
def generate_feature_graphic():
    img = Image.new("RGB", (1024, 500), (11, 17, 32)) # Midnight Navy
    draw = ImageDraw.Draw(img)
    
    # Draw subtle background grid & tech circles
    for r in [400, 320, 240, 160]:
        draw.ellipse([240 - r, 250 - r, 240 + r, 250 + r], outline=(22, 34, 56), width=1)
        
    # Draw Logo on left
    draw_jarvis_logo(draw, 240, 250, 140)
    
    try:
        font_large = ImageFont.truetype("arialbd.ttf", 46)
        font_sub = ImageFont.truetype("arialbd.ttf", 26)
        font_tag = ImageFont.truetype("arial.ttf", 19)
    except:
        font_large = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_tag = ImageFont.load_default()
        
    text_x = 440
    draw.text((text_x, 120), "MY MONEY MANAGER", font=font_large, fill=(255, 255, 255))
    draw.text((text_x, 180), "Powered by Jarvis AI Assistant", font=font_sub, fill=(56, 189, 248))
    
    # Feature Bullet Chips
    chips = [
        "Automatic HDFC Bank SMS Detection",
        "Hands-Free Continuous Voice Entry",
        "Multi-Account Fund Buckets & Editing",
        "Lending, Borrowing & Repayment Tracker",
        "100% Offline & Private - No Cloud Needed"
    ]
    
    y = 240
    for chip in chips:
        draw.ellipse([text_x, y + 4, text_x + 10, y + 14], fill=(16, 185, 129))
        draw.text((text_x + 22, y), chip, font=font_tag, fill=(226, 232, 240))
        y += 34
        
    # Royal Blue accent border on bottom
    draw.rectangle([0, 492, 1024, 500], fill=(37, 99, 235))

    img.save("distribution/feature_graphic_1024x500.png", "PNG")
    print("Generated distribution/feature_graphic_1024x500.png (1024x500)")

# 3. GENERATE PROPORTIONED PORTRAIT PROMO (1080 x 1920)
def generate_promo_portrait():
    img = Image.new("RGB", (1080, 1920), (11, 17, 32))
    draw = ImageDraw.Draw(img)
    
    # Logo at top
    draw_jarvis_logo(draw, 540, 360, 200)
    
    try:
        font_title = ImageFont.truetype("arialbd.ttf", 64)
        font_hero = ImageFont.truetype("arialbd.ttf", 44)
        font_body = ImageFont.truetype("arial.ttf", 32)
        font_card_t = ImageFont.truetype("arialbd.ttf", 34)
        font_card_b = ImageFont.truetype("arial.ttf", 26)
    except:
        font_title = ImageFont.load_default()
        font_hero = ImageFont.load_default()
        font_body = ImageFont.load_default()
        font_card_t = ImageFont.load_default()
        font_card_b = ImageFont.load_default()

    draw.text((540, 640), "MY MONEY MANAGER", font=font_title, fill=(255, 255, 255), anchor="mm")
    draw.text((540, 720), "Your Personal Jarvis Financial Assistant", font=font_hero, fill=(56, 189, 248), anchor="mm")
    
    # Cards displaying key core pillars
    cards = [
        ("Instant HDFC Bank SMS Capture", "Never forget an expense. Reads debit & credit SMS and creates pending drafts for your 1-tap review.", (37, 99, 235)),
        ("Continuous Hands-Free Voice Entry", "Speak naturally in English, Hindi or Gujarati. Jarvis asks clarifications and listens automatically.", (16, 185, 129)),
        ("Full Account & Fund Management", "Add, edit names, adjust initial opening balances, transfer between funds, and delete accounts.", (2, 132, 199)),
        ("Zero Cloud, 100% Device Privacy", "All financial records and ledger balances reside locally in SQLite on your phone. No tracking.", (239, 68, 68))
    ]
    
    start_y = 830
    for title, desc, accent in cards:
        # Card background
        draw.rounded_rectangle([90, start_y, 990, start_y + 190], radius=24, fill=(20, 27, 45), outline=(30, 41, 59), width=2)
        # Accent indicator
        draw.rounded_rectangle([110, start_y + 24, 118, start_y + 80], radius=4, fill=accent)
        draw.text((140, start_y + 30), title, font=font_card_t, fill=(255, 255, 255))
        draw.text((140, start_y + 85), desc, font=font_card_b, fill=(148, 163, 184))
        start_y += 230
        
    draw.text((540, 1820), "Daily 9 PM Reminder  •  Modern Royal Blue  •  Exact Paise Ledger", font=font_body, fill=(100, 116, 139), anchor="mm")
    
    img.save("distribution/promo_portrait_1080x1920.png", "PNG")
    print("Generated distribution/promo_portrait_1080x1920.png (1080x1920)")

generate_icon_512()
generate_feature_graphic()
generate_promo_portrait()
