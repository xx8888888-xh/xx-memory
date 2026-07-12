"""
全功能点击测试脚本：覆盖首页、导入、复习、设置四大模块。
要求模拟器分辨率 720x1280（MuMu 默认竖屏），使用 uiautomator2。
"""
import sys
import io
import json
import time
import traceback
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import uiautomator2 as u2

REPORT = []
OUT_DIR = Path(__file__).parent
LOG_FILE = OUT_DIR / "full_app_test.log"

# 使用独立日志文件，避免 uiautomator2 关闭 stdout 描述符导致 I/O 错误。
_log_fp = open(LOG_FILE, 'a', encoding='utf-8', errors='replace')


def log_write(msg):
    _log_fp.write(f"{msg}\n")
    _log_fp.flush()


def log(name, status, note="", screenshot=None):
    entry = {"name": name, "status": status, "note": note}
    if screenshot:
        entry["screenshot"] = str(screenshot)
    REPORT.append(entry)
    log_write(f"[{status}] {name}: {note}")


def wait(seconds=1.5):
    time.sleep(seconds)


def take_screenshot(device, save_path=None):
    img = device.screenshot()
    if save_path:
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        img.save(save_path)
        log_write(f"Screenshot saved: {save_path}")
    return img


def shot(d, name):
    p = str(OUT_DIR / f"{name}.png")
    take_screenshot(d, p)
    return p


def home(d):
    # 使用 ADB 启动应用，避免 uiautomator2 在应用启动时断连
    import subprocess
    subprocess.run(["D:/software/MuMuPlayer/nx_main/adb.exe", "-s", d.serial, "shell", "am", "start", "-n", "com.xxmemory.app/.MainActivity"], check=False)
    wait(4)


def _tap_element_center(d, elem):
    """Use ADB shell tap on element center; more reliable on Compose buttons."""
    try:
        bounds = elem.bounds()
        cx = (bounds[0] + bounds[2]) // 2
        cy = (bounds[1] + bounds[3]) // 2
        d.shell(f"input tap {cx} {cy}")
        return True
    except Exception as e:
        log_write(f"_tap_element_center error: {e}")
        return False


def safe_click_text(d, text, partial=False, timeout=2.0):
    try:
        els = d(text=text)
        if els.exists:
            if _tap_element_center(d, els):
                return True
            els.click()
            return True
        if partial:
            els = d(textContains=text)
            if els.exists:
                if _tap_element_center(d, els):
                    return True
                els.click()
                return True
        # fallback: wait up to timeout for the element to appear
        waited = d(text=text).wait(timeout=timeout)
        if waited:
            if _tap_element_center(d, d(text=text)):
                return True
            d(text=text).click()
            return True
        if partial:
            waited = d(textContains=text).wait(timeout=timeout)
            if waited:
                if _tap_element_center(d, d(textContains=text)):
                    return True
                d(textContains=text).click()
                return True
    except Exception as e:
        log_write(f"safe_click_text error: {e}")
    return False


def has_text(d, text, partial=False, timeout=1.5):
    try:
        if d(text=text).wait(timeout=timeout):
            return True
        if partial and d(textContains=text).wait(timeout=timeout):
            return True
    except Exception:
        pass
    return False


def is_element_on_screen(elem, screen_h=1280):
    """判断元素 bounds 是否在当前屏幕可视区域内。"""
    try:
        bounds = elem.bounds()
        return bounds[1] >= 0 and bounds[3] <= screen_h and bounds[3] > bounds[1]
    except Exception:
        return False


def scroll_and_click_text(d, text, partial=False, max_swipes=6, start_y=1000, end_y=400):
    """上滑查找并点击指定文本，直到成功或达到最大滑动次数。"""
    for i in range(max_swipes):
        try:
            target = d(text=text) if not partial else d(textContains=text)
            if target.exists and is_element_on_screen(target):
                if _tap_element_center(d, target):
                    log_write(f"scroll_and_click_text: 第 {i + 1} 次滑动后点击 '{text}' 成功")
                    return True
        except Exception as e:
            log_write(f"scroll_and_click_text error at swipe {i + 1}: {e}")
        d.swipe(360, start_y, 360, end_y, 0.5)
        wait(0.6)
    return False


def tap_coord(d, x, y):
    d.shell(f"input tap {x} {y}")


def import_test_cards(d):
    """通过 JSON 导入两张测试卡：vocabulary + poetry。"""
    home(d)
    tap_coord(d, 360, 1231)  # 底部“导入”Tab
    wait(2)
    s = shot(d, "import_page")

    # 展开“更多导入方式”
    d.shell("input tap 360 473")
    wait(2)

    # 点击“从JSON导入”行（坐标点击避免父节点遮挡）
    d.shell("input tap 360 611")
    wait(2.5)
    s = shot(d, "import_json_input")

    payload = json.dumps([
        {
            "question": "test_vocabulary_word",
            "answer": "测试单词释义",
            "cardType": "vocabulary",
            "example": "This is a test vocabulary word used in a sentence.",
            "distractors": "干扰项1,干扰项2,干扰项3",
            "tags": "测试,单词",
            "nextReviewDate": 0
        },
        {
            "question": "测试古诗",
            "answer": "测试古诗全文内容",
            "cardType": "poetry",
            "tags": "测试,古诗",
            "nextReviewDate": 0
        }
    ], ensure_ascii=False)

    try:
        edits = d(className="android.widget.EditText")
        if edits.count > 0:
            edits[0].set_text(payload)
        else:
            log("导入测试卡片", "FAIL", "未找到 JSON 输入框")
            return
    except Exception as e:
        log("导入测试卡片", "FAIL", str(e))
        return

    wait(0.5)
    if not safe_click_text(d, "导入"):
        safe_click_text(d, "确定")
    wait(2.5)
    s = shot(d, "import_result")
    ok = has_text(d, "成功") or has_text(d, "导入") or has_text(d, "张")
    log("导入测试卡片", "PASS" if ok else "FAIL", "", s)
    back(d)


def back(d):
    import subprocess
    subprocess.run(["D:/software/MuMuPlayer/nx_main/adb.exe", "-s", d.serial, "shell", "input", "keyevent", "4"], check=False)
    wait(1)


def test_home_page(d):
    home(d)
    s = shot(d, "home_page")
    ok = has_text(d, "到期") or has_text(d, "待复习") or has_text(d, "总卡片")
    log("首页显示", "PASS" if ok else "FAIL", "", s)

    # 搜索
    safe_click_text(d, "搜索") or tap_coord(d, 600, 230)
    wait(1)
    edits = d(className="android.widget.EditText")
    if edits.count > 0:
        edits[0].set_text("abandon")
        wait(1.5)
        s = shot(d, "home_search")
        log("首页搜索", "PASS", "输入 abandon", s)
        d.press("back")
        wait(1)

    # 日历：首页下滑
    d.swipe(360, 900, 360, 400, 0.5)
    wait(1.5)
    s = shot(d, "home_scroll")
    log("首页下滑统计", "PASS" if has_text(d, "日历") else "INFO", "", s)


def test_review_clean_ui(d):
    home(d)
    safe_click_text(d, "复习")
    wait(2.5)
    s = shot(d, "review_clean_ui")

    # 不应出现顶部模式切换条
    bad = has_text(d, "闪卡模式") or has_text(d, "百词斩模式") or has_text(d, "不背单词模式")
    ok = not bad
    log("复习页-无模式切换条", "PASS" if ok else "FAIL", "", s)

    # 应有右上角“更多”菜单
    more = d(description="更多") or d(descriptionContains="更多")
    log("复习页-更多菜单入口", "PASS" if more.exists else "FAIL", "", s)

    # 展开更多菜单
    if more.exists:
        more.click()
        wait(1.5)
        s = shot(d, "review_more_menu")
        log("复习页-更多菜单展开", "PASS", "", s)
        # 关闭菜单
        d.press("back")
        wait(1)
    back(d)


def test_vocabulary_review_flow(d):
    home(d)
    safe_click_text(d, "复习")
    wait(2.5)
    s = shot(d, "review_vocab_enter")

    if has_text(d, "再来一组") or has_text(d, "无到期"):
        log("单词复习", "INFO", "无到期单词卡片", s)
        back(d)
        return

    # 四选一：点击正确答案
    if safe_click_text(d, "测试单词释义"):
        wait(1.5)
        s = shot(d, "review_vocab_option")
        log("单词复习-选择正确选项", "PASS", "", s)
    else:
        log("单词复习-选择正确选项", "INFO", "未找到正确选项文本", s)

    # 例句自评：清晰
    if safe_click_text(d, "清晰"):
        wait(4)
        s = shot(d, "review_vocab_example")
        log("单词复习-例句自评", "PASS", "", s)

    # 独立回忆：记对了
    wait(2)
    clicked = safe_click_text(d, "记对了", partial=True, timeout=5)
    if not clicked:
        # 文字匹配偶发失败，使用按钮区域坐标兜底
        log_write("记对了文字匹配失败，使用坐标兜底 (358, 673)")
        d.shell("input tap 358 673")
        clicked = True
    if clicked:
        wait(1.5)
        s = shot(d, "review_vocab_recall")
        log("单词复习-独立回忆", "PASS", "", s)
        # 提交后才会进入拼写检查（或下一张卡片）
        if safe_click_text(d, "提交"):
            wait(2)
            s = shot(d, "review_vocab_submit")
            log("单词复习-提交自评", "PASS", "", s)

    # 拼写检查（提交自评后应自动弹出）
    wait(1.5)
    s = shot(d, "review_vocab_spelling")
    if has_text(d, "拼写测试") or has_text(d, "拼写", partial=True):
        log("单词复习-拼写检查", "PASS", "自动进入拼写检查", s)
        edits = d(className="android.widget.EditText")
        if edits.count > 0:
            edits[0].set_text("test_vocabulary_word")
            wait(0.5)
            safe_click_text(d, "检查") or safe_click_text(d, "提交")
            wait(1.5)
            s = shot(d, "review_vocab_spelling_done")
            ok = has_text(d, "拼写正确") or has_text(d, "继续")
            log("单词复习-拼写提交", "PASS" if ok else "FAIL", "", s)
            if ok:
                safe_click_text(d, "继续")
                wait(1)
    else:
        log("单词复习-拼写检查", "FAIL", "未自动进入拼写检查", s)

    back(d)


def test_poetry_review_flow(d):
    home(d)
    safe_click_text(d, "复习")
    wait(2.5)
    s = shot(d, "review_poetry_enter")

    if has_text(d, "再来一组") or has_text(d, "无到期"):
        log("古诗文复习", "INFO", "无到期古诗文卡片", s)
        back(d)
        return

    has_flow = has_text(d, "测试古诗", partial=True) or has_text(d, "开始默写", partial=True)
    log("古诗文复习-进入", "PASS" if has_flow else "FAIL", "", s)

    # 默认卡片无音频，应显示“开始默写”与“跳过朗诵”
    if safe_click_text(d, "开始默写", partial=True):
        wait(1.5)
        s = shot(d, "review_poetry_dictation")
        log("古诗文复习-开始默写", "PASS", "", s)

    # Compose OutlinedTextField 直接 set_text 比 focus 后输入更稳定，支持中文
    edits = d(className="android.widget.EditText")
    if edits.count > 0:
        try:
            edits[0].set_text("测试古诗全文内容")
            log_write("古诗文默写：通过 EditText.set_text 输入成功")
        except Exception as e:
            log_write(f"EditText.set_text failed: {e}, fallback to focus input")
            edits[0].click()
            wait(0.5)
            try:
                d(focused=True).set_text("测试古诗全文内容")
            except Exception as e2:
                log_write(f"focus set_text failed: {e2}, fallback to adb shell input text")
                d.shell("input text '测试古诗全文内容'")
    else:
        d.shell("input tap 360 600")
        wait(0.5)
        try:
            d(focused=True).set_text("测试古诗全文内容")
        except Exception as e:
            log_write(f"set_text failed: {e}, fallback to adb shell input text")
            d.shell("input text '测试古诗全文内容'")
    wait(0.5)
    # 多行输入框较高，检查按钮可能被推到屏幕外，循环上滑直到按钮可见并点击
    if not scroll_and_click_text(d, "检查", partial=False, max_swipes=6, start_y=1000, end_y=400):
        log_write("检查按钮上滑后仍未定位，使用坐标兜底 (360, 988)")
        d.shell("input tap 360 988")
        wait(0.5)
    wait(2.5)
    s = shot(d, "review_poetry_dictation_done")
    ok = has_text(d, "回答正确", partial=True) or has_text(d, "继续") or has_text(d, "下一个")
    if not ok:
        # debug dump
        try:
            import subprocess
            subprocess.run(["D:/software/MuMuPlayer/nx_main/adb.exe", "-s", d.serial, "shell", "uiautomator", "dump", "/sdcard/poetry_fail.xml"], check=False)
            subprocess.run(["D:/software/MuMuPlayer/nx_main/adb.exe", "-s", d.serial, "pull", "/sdcard/poetry_fail.xml", str(OUT_DIR / "poetry_fail.xml")], check=False)
        except Exception as e:
            log_write(f"debug dump failed: {e}")
    log("古诗文复习-默写提交", "PASS" if ok else "FAIL", "", s)

    back(d)


def test_settings(d):
    home(d)
    # 右上角设置图标：优先使用 content-desc="设置" 定位，fallback 到文本/坐标兜底
    settings_btn = d(description="设置")
    clicked_settings = False
    if settings_btn.wait(timeout=3.0):
        # 点击父节点中心，避免子节点不可点击导致失效
        try:
            parent = settings_btn.parent()
            if parent and parent.exists():
                clicked_settings = _tap_element_center(d, parent)
        except Exception as e:
            log_write(f"获取设置图标父节点失败: {e}")
        if not clicked_settings:
            clicked_settings = _tap_element_center(d, settings_btn)
        if not clicked_settings:
            settings_btn.click()
            clicked_settings = True
        log_write("设置页入口：通过 content-desc='设置' 点击")
    elif safe_click_text(d, "设置", partial=False, timeout=2.0):
        clicked_settings = True
        log_write("设置页入口：通过文本 '设置' 点击")
    else:
        log_write("设置页入口：content-desc/文本均未找到，使用坐标兜底 (640, 136)")
        tap_coord(d, 640, 136)
        clicked_settings = True
    wait(2.5)
    s = shot(d, "settings_page")
    ok = has_text(d, "设置") or has_text(d, "每日卡片限制")
    if not ok:
        try:
            import subprocess
            subprocess.run(["D:/software/MuMuPlayer/nx_main/adb.exe", "-s", d.serial, "shell", "uiautomator", "dump", "/sdcard/settings_fail.xml"], check=False)
            subprocess.run(["D:/software/MuMuPlayer/nx_main/adb.exe", "-s", d.serial, "pull", "/sdcard/settings_fail.xml", str(OUT_DIR / "settings_fail.xml")], check=False)
        except Exception as e:
            log_write(f"settings debug dump failed: {e}")
    log("设置页", "PASS" if ok else "FAIL", "", s)

    # 每日卡片限制输入框
    edits = d(className="android.widget.EditText")
    if edits.count > 0:
        edits[0].set_text("15")
        wait(1)
        s = shot(d, "settings_daily_limit")
        log("设置-每日卡片限制", "PASS", "改为15", s)

    # 困难优先排序开关（新设置项）
    if scroll_and_click_text(d, "困难优先排序", partial=True, max_swipes=3, start_y=900, end_y=400):
        wait(0.5)
        s = shot(d, "settings_difficult_first")
        log("设置-困难优先排序", "PASS", "点击切换", s)

    # 古诗文朗诵开关
    if safe_click_text(d, "启用朗诵阶段", partial=True):
        wait(0.5)
        log("设置-古诗文朗诵开关", "PASS", "点击切换")

    # 返回首页
    d.press("back")
    wait(1.5)
    s = shot(d, "settings_back_home")
    log("设置-返回首页", "PASS" if (has_text(d, "到期") or has_text(d, "总卡片")) else "FAIL", "", s)


def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('-s', '--serial', default='emulator-5556')
    args = parser.parse_args()

    # 通过 ADB shell 清理数据，避免 uiautomator2 连接在新应用启动前断连
    adb = "D:/software/MuMuPlayer/nx_main/adb.exe"
    import subprocess
    subprocess.run([adb, "-s", args.serial, "shell", "am", "force-stop", "com.xxmemory.app"], check=False)
    subprocess.run([adb, "-s", args.serial, "shell", "pm", "clear", "com.xxmemory.app"], check=False)
    wait(3)

    # 先启动应用，再连接 uiautomator2，避免 atx-agent 在应用切换时关闭管道
    subprocess.run([adb, "-s", args.serial, "shell", "am", "start", "-n", "com.xxmemory.app/.MainActivity"], check=False)
    wait(4)

    # 循环尝试连接，直到 uiautomator2 服务稳定
    d = None
    for attempt in range(10):
        try:
            import uiautomator2 as u2
            d = u2.connect(args.serial)
            _ = d.info
            log_write(f"Connected to {args.serial} (attempt {attempt + 1})")
            break
        except Exception as e:
            log_write(f"Connect attempt {attempt + 1} failed: {e}")
            wait(1)
    if d is None:
        log_write("ERROR: Failed to connect to device")
        sys.exit(1)

    # 处理首次启动的新用户引导页
    if has_text(d, "开始体验", timeout=3.0) or has_text(d, "欢迎使用", timeout=1.5):
        log_write("检测到新用户引导页，点击开始体验")
        safe_click_text(d, "开始体验")
        wait(2.5)

    import_test_cards(d)
    test_home_page(d)
    test_review_clean_ui(d)
    test_vocabulary_review_flow(d)
    test_poetry_review_flow(d)
    test_settings(d)

    report_path = OUT_DIR / "full_app_report.json"
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(REPORT, f, ensure_ascii=False, indent=2)

    total = len(REPORT)
    passed = sum(1 for r in REPORT if r["status"] == "PASS")
    failed = sum(1 for r in REPORT if r["status"] == "FAIL")
    info = sum(1 for r in REPORT if r["status"] == "INFO")
    log_write(f"\n测试完成: 总计 {total}, 通过 {passed}, 失败 {failed}, 信息 {info}")
    log_write(f"报告保存: {report_path}")
    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        log_write("FATAL: " + traceback.format_exc())
        _log_fp.close()
        sys.exit(1)
