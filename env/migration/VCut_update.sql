INSERT INTO bulk_distribution(user_id, bot_name, message_ru, message_en, message_uz)
             SELECT user_id, 'convert_any2any_bot', '#Update
1. Добавлена функция <b>обрезки видео</b>. Используйте команду /vcut для этого.
2. Добавлена функция создания <b>круглого видео</b>. Отправьте видео и выберите для этого <b>VIDEO_NOTE</b> на клавиатуре. Для создания круглого видео ваше видео должно быть <b>квадратным</b>, продолжительностью не более <b>60 секунд</b>, размером не более <b>8МБ</b> и в формате <b>MP4</b>.
3. Добавлена функция создания <b>квадратного видео</b>. Отправьте видео и выберите для этого <b>SQUARE</b> на клавиатуре. Например, такое видео может быть использовано для создания <b>круглого видео</b>.
4. Добавлена функция извлечения субтитров из видео в форматах: <b>ASS, SRT, SUBRIP, WEBVTT</b>.
5. Добавлена функция <b>удаления аудио из видео</b>. Отправьте видео и выберите для этого <b>MUTE</b> на клавиатуре.
6. Добавлена функция <b>получения метаданных видео</b> таких как формат, разрешение, длина, размер. Отправьте видео и выберите для этого <b>PROBE</b> на клавиатуре.
7. Ускорена конвертация некоторых видео.
8. Улучшено расположение кнопок на клавиатуре при конвертации.
8. Исправлены другие мелкие недочеты.', '#Update
1. Added <b>video cutting</b> function. Use the /vcut command for this.
2. Added the function to create <b>video note</b>. Submit your video and choose <b>VIDEO_NOTE</b> on the keyboard to do this. To create a video note your video must be <b>square</b>, no more than <b>60 seconds</b>, no more than <b>8MB</b> in size and must be in <b>MP4</b> format.
3. Added function to create <b>square video</b>. Submit your video and choose <b>SQUARE</b> on the keyboard to do this. For example, such a video can be used to create <b>video note</b>.
4. Added the function to extract subtitles from video in the following formats: <b>ASS, SRT, SUBRIP, WEBVTT</b>.
5. Added function to <b>remove audio from video</b>. Submit your video and choose <b>MUTE</b> on the keyboard to do this.
6. Added function to <b>get video metadata</b> such as <b>format, resolution, length, size</b>. Submit your video and choose <b>PROBE</b> on the keyboard.
7. Increased conversion speed of some videos.
8. Improved the arrangement of buttons on the keyboard when converting.
8. Fixed other minor bugs.', '#Yangilash
1. <b>Videoni kesish</b> funktsiyasi qo''shildi. Buning uchun /vcut buyrug''idan foydalaning.
2. <b>Dumaloq video</b> yaratish funktsiyasi qo''shildi. Buning uchun videongizni yuboring va klaviaturadan <b>VIDEO_NOTE</b> -ni tanlang. Dumaloq videoni yaratish uchun videongiz <b>kvadrat</b>, <b>60 soniya</b>, <b>8MB</b> va <b>MP4</b> bo''lishi kerak.
3. <b>Kvadrat video</b> yaratish uchun funktsiya qo''shildi. Buning uchun videongizni yuboring va klaviaturada <b>SQUARE</b> ni tanlang. Masalan, bunday videodan <b>dumaloq video</b> yaratish mumkin.
4. Quyidagi formatlarda videodan subtitrlarni chiqarish funktsiyasi qo''shildi: <b>ASS, SRT, SUBRIP, WEBVTT</b>.
5. <b>Videodan audio olib tashlash</b> funktsiyasi qo''shildi. Videongizni yuboring va klaviaturadan <b>MUTE</b> ni tanlang.
6. Video meta-ma''lumotlarini olish uchun funktsiya qo''shildi, masalan <b>format, o''lcham, uzunlik, o''lcham</b>. Videongizni yuboring va klaviaturada <b>PROBE</b> -ni tanlang.
7. Ba''zi videolarni tezkor konvertatsiya qilish.
8. Konvertatsiya qilishda klaviaturadagi tugmachalarning joylashuvi yaxshilandi.
8. Boshqa kichik xatolar tuzatildi.'
             from tg_user where blocked = false;