#!/usr/bin/env python3
"""Вносит небольшие правки для MobileGlues в существующие файлы форка.

Запуск (из папки репозитория, где лежит app_pojavlauncher):
    python apply_mobileglues.py .

Скрипт безопасен к повторному запуску: если правка уже есть, он её пропускает.
Если не находит нужное место в файле, ничего не ломает и пишет, что править
нужно вручную.
"""
import re
import sys
from pathlib import Path

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
main = root / "app_pojavlauncher" / "src" / "main"
ok, failed = [], []


def edit(path, fn, label):
    p = main / path
    if not p.exists():
        failed.append(f"{label}: файл не найден ({path})")
        return
    text = p.read_text(encoding="utf-8")
    new = fn(text)
    if new is None:
        failed.append(f"{label}: не нашёл место для вставки, правь вручную")
    elif new == text:
        ok.append(f"{label}: уже сделано")
    else:
        p.write_text(new, encoding="utf-8")
        ok.append(f"{label}: готово")


def add_array_item(text, array_name, item):
    m = re.search(r'(<string-array name="%s">)(.*?)(</string-array>)' % re.escape(array_name), text, re.S)
    if not m:
        return None
    if item in m.group(2):
        return text
    body = m.group(2).rstrip() + "\n        " + item + "\n    "
    return text[:m.start(2)] + body + text[m.end(2):]


# 1. Список рендереров (названия и значения)
def headings(text):
    count = len(re.findall(r"<item", re.search(r'<string-array name="renderer">(.*?)</string-array>', text, re.S).group(1))) if re.search(r'<string-array name="renderer">', text) else 0
    if "mobileglues" in text and ">MobileGlues<" in text:
        step1 = text
    else:
        step1 = add_array_item(text, "renderer", '<item name="%d">MobileGlues</item>' % (count + 1))
    if step1 is None:
        return None
    return add_array_item(step1, "renderer_values", "<item>mobileglues</item>")


edit("res/values/headings_array.xml", headings, "Список рендереров")


# 2. Подключение библиотеки рендерера и переменных окружения (JREUtils)
def jre(text):
    if '"mobileglues"' in text:
        return text
    case_block = (
        '            case "mobileglues":\n'
        '                renderLibrary = "libmobileglues.so";\n'
        '                useGles = true;\n'
        '                glesVersion = 3;\n'
        '                break;\n'
    )
    m = re.search(r'^[ \t]*case "opengles3_ltw"\s*:', text, re.M)
    if not m:
        return None
    text = text[:m.start()] + case_block + text[m.start():]
    env_block = (
        '        if ("mobileglues".equals(renderer)) {\n'
        '            MobileGluesConfig.configureEnvironment(context, envMap);\n'
        '        }\n\n'
    )
    m = re.search(r'^[ \t]*setupAngleEnv\(', text, re.M)
    if not m:
        return None
    return text[:m.start()] + env_block + text[m.start():]


edit("java/net/kdt/pojavlaunch/utils/JREUtils.java", jre, "JREUtils (запуск рендерера)")


# 3. Проверка совместимости с устройством
def compat(text):
    if "mobileglues" in text:
        return text
    m = re.search(r'^([ \t]*if\(rendererId\.contains\("ltw"\).*\n)', text, re.M)
    if not m:
        return None
    line = '            if(rendererId.contains("mobileglues") && !deviceHasOpenGLES3) continue;\n'
    return text[:m.end()] + line + text[m.end():]


edit("java/net/kdt/pojavlaunch/utils/RendererCompatUtil.java", compat, "RendererCompatUtil (список для устройства)")


# 4. Пункт «Настройки MobileGlues» в экране видео
def pref_video(text):
    if "mobileglues_settings" in text:
        return text
    m = re.search(r'<androidx\.preference\.ListPreference[^>]*android:key="renderer"[^>]*/>', text, re.S)
    if not m:
        return None
    block = (
        '\n\n        <Preference\n'
        '            android:key="mobileglues_settings"\n'
        '            android:title="@string/mobileglues_settings_title"\n'
        '            android:summary="@string/mobileglues_settings_description"\n'
        '            android:fragment="net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceMobileGluesFragment"\n'
        '            android:icon="@drawable/ic_px_settings" />'
    )
    return text[:m.end()] + block + text[m.end():]


edit("res/xml/pref_video.xml", pref_video, "Экран видео (пункт настроек)")


# 5. Показывать пункт только если рендерер доступен на устройстве
def video_fragment(text):
    if "mobileglues_settings" in text:
        return text
    snippet = (
        '\n        Preference mobileGluesSettings = requirePreference("mobileglues_settings");\n'
        '        mobileGluesSettings.setVisible(RendererCompatUtil.getCompatibleRenderers(requireContext())\n'
        '                .rendererIds.contains("mobileglues"));\n'
    )
    idx = text.rstrip().rfind("\n    }\n}")
    if idx == -1:
        return None
    text = text[:idx + 1] + snippet.lstrip("\n") + text[idx + 1:]
    for imp in ("androidx.preference.Preference", "net.kdt.pojavlaunch.utils.RendererCompatUtil"):
        if "import %s;" % imp not in text:
            text = re.sub(r"(^package .*?;\n)", r"\1\nimport %s;\n" % imp, text, count=1, flags=re.M)
    return text


edit("java/net/kdt/pojavlaunch/prefs/screens/LauncherPreferenceVideoFragment.java", video_fragment, "Фрагмент видео")

# Проверка иконок, которые нужны для pref_mobileglues.xml
for icon in ("ic_px_image_renderer", "ic_px_settings", "ic_px_speed"):
    if not list((main / "res").glob(f"drawable*/{icon}.*")):
        failed.append(f"Нет иконки {icon}: замени её в pref_mobileglues.xml на любую существующую")

print("\n".join("[ok] " + s for s in ok))
print("\n".join("[!!] " + s for s in failed))
sys.exit(1 if failed else 0)
