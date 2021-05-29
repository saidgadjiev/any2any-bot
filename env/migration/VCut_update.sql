INSERT INTO bulk_distribution(user_id, bot_name, message_ru, message_en, message_uz)
SELECT user_id, 'SmartVideoConverterBot', '#Update
1. Добавлена функция <b>обрезки видео</b>. Используйте команду /vcut для этого.
2. Добавлена функция <b>удаления аудио из видео</b>. Отправьте видео и выберите для этого <b>MUTE</b> на клавиатуре.
3. Добавлена функция <b>получения метаданных видео</b> таких как формат, разрешение, длина, размер. Отправьте видео и выберите для этого <b>PROBE</b> на клавиатуре.
4. Ускорена конвертация некоторых видео.
5. Исправлены другие мелкие недочеты.', '#Update
1. Added <b>cut video</b> function. Use the /vcut command for this.
2. Added <b>remove audio from video</b> function. Send me your video and choose <b>MUTE</b> on the keyboard.
3. Added <b>get video metadata</b> function. Such as format, resolution, length, size. Send me your video and choose <b>PROBE</b> on the keyboard.
4. Increased speed of conversion of some videos.
5. Fixed other minor bugs.', '#Yangilash
1. <b>Videoni kesish</b> funktsiyasi qo''shildi. Buning uchun /vcut buyrug''idan foydalaning.
2. <b>Ovozni videodan olib tashlash</b> uchun funktsiya qo''shildi. Videongizni yuboring va buni amalga oshirish uchun klaviaturadan <b>MUTE</b> tugmasini bosing.
3. Format, piksellar sonini, uzunlik, o''lcham kabi videofilmlarni olish uchun funktsiya qo''shildi. Videongizni yuboring va klaviaturadan <b>PROBE</b> ni tanlang.
4. Ba''zi videolarni o''zgartirish tezlashtirildi.
5. Boshqa kichik xatolar tuzatildi.'
from tg_user;