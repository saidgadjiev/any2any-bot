alter table conversion_queue
    add converter VARCHAR(16);
update conversion_queue
set converter = 'video'
where files[1].format in
      ('TS', 'WMV', 'M4V', 'WEBM', '_3GP', 'FLV', 'MPG', 'MKV', 'AVI', 'VOB', 'MP4', 'MOV',
       'MTS', 'MPEG');

update conversion_queue
set converter = 'audio'
where files[1].format in
      ('WMA', 'OGG', 'MID', 'AIFF', 'AMR', 'RA', 'MP3', 'RM', 'FLAC', 'SPX', 'M4B', 'WAV', 'AAC', 'M4A',
       'OPUS');

update conversion_queue
set converter = 'document'
WHERE files[1].format in
      ('GIF', 'AZW4', 'DOTX', 'PDF', 'TXT', 'XLAM', 'CGM', 'RTF', 'URL', 'POTX', 'SXC', 'AZW3', 'CHM', 'XLSB',
       'HTMLZ', 'POT', 'PRC', 'XPS', 'PS', 'XLTM', 'DOC', 'CBR', 'DJVU', 'NUMBERS', 'PCL',
       'TGS', 'ODT', 'DOT', 'DOTM', 'PDF_IMPORT', 'JPG', 'TSV', 'CBZ', 'HEIC', 'OEB', 'CSV', 'AZW', 'RB', 'SWF', 'XLS',
       'HTML', 'BMP', 'PMLZ', 'MOBI', 'POTM', 'PPTX', 'DOCX', 'TIFF', 'PML', 'SVG', 'CBC', 'TCR', 'XLSM', 'MHT', 'PNG',
       'ICO', 'LRF', 'PHOTO', 'TXTZ', 'FODS', 'OTP', 'ODS', 'PPS', 'FBZ', 'SNB', 'FB2',
       'DOCM', 'HEIF', 'EPUB', 'DIF', 'XML', 'OTT', 'PPSX', 'TEXT', 'PDB', 'PPTM', 'XLTX', 'PPSM', 'MHTML', 'WEBP',
       'ODP', 'XLSX', 'LIT', 'PPT', 'JP2');

update conversion_queue
set converter = 'all'
where converter is null and status not in(0,1);

alter table conversion_queue alter column converter set not null;