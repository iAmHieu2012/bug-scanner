import os
import requests
import re

# Lấy thông tin đăng nhập từ môi trường ẩn của GitHub
username = os.environ['INAT_USERNAME']
password = os.environ['INAT_PASSWORD']

def get_token_via_scraping():
    print("1. Khởi tạo phiên làm việc (Session)...")
    session = requests.Session()
    
    # Giả lập User-Agent giống trình duyệt thật để không bị block
    session.headers.update({
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36'
    })

    print("2. Tải trang đăng nhập để lấy CSRF Token...")
    login_page_url = "https://www.inaturalist.org/users/sign_in"
    response = session.get(login_page_url)
    response.raise_for_status()

    # Móc CSRF token ẩn trong HTML
    match = re.search(r'<meta name="csrf-token" content="([^"]+)"', response.text)
    if not match:
        raise Exception("Không tìm thấy CSRF Token. Có thể iNaturalist đã đổi giao diện!")
    csrf_token = match.group(1)

    print("3. Gửi thông tin đăng nhập...")
    payload = {
        "authenticity_token": csrf_token,
        "user[email]": username,
        "user[password]": password,
        "commit": "Log In"
    }
    
    login_post = session.post(login_page_url, data=payload)
    
    # Kiểm tra xem có bị kẹt lại trang sign_in không
    if "sign_in" in login_post.url:
        raise Exception("Đăng nhập thất bại! Kiểm tra lại Username hoặc Password.")

    print("4. Đăng nhập thành công! Đang lấy JWT API Token...")
    token_response = session.get("https://www.inaturalist.org/users/api_token")
    token_response.raise_for_status()
    
    api_token = token_response.json().get("api_token")
    if not api_token:
        raise Exception("Không trích xuất được api_token từ JSON.")
        
    return api_token

if __name__ == "__main__":
    try:
        new_token = get_token_via_scraping()
        
        # Đẩy token lên Firestore
        service_account_str = os.getenv('FIREBASE_SERVICE_ACCOUNT_KEY')
        if service_account_str:
            import json
            import firebase_admin
            from firebase_admin import credentials, firestore

            print("5. Đăng nhập và cập nhật token lên Firestore...")
            cred_dict = json.loads(service_account_str)
            cred = credentials.Certificate(cred_dict)
            firebase_admin.initialize_app(cred)

            db = firestore.client()
            db.collection('configs').document('inaturalist').set({
                'api_token': new_token,
                'updated_at': firestore.SERVER_TIMESTAMP
            })
            print("Cập nhật Firestore thành công!")
        else:
            print("Không tìm thấy biến FIREBASE_SERVICE_ACCOUNT_KEY, bỏ qua cập nhật Firestore.")

        # Đẩy token này vào biến môi trường tạm của GitHub Actions
        env_file = os.getenv('GITHUB_ENV')
        if env_file:
            with open(env_file, "a") as f:
                f.write(f"NEW_INAT_TOKEN={new_token}\n")
        
        print("Lấy token thành công!")
    except Exception as e:
        print(f"Lỗi: {e}")
        exit(1)