package ru.gadjini.any2any.service.cleaner;

import ru.gadjini.any2any.bot.command.keyboard.ConvertMaker;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.conversion.impl.Epub2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.Excel2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.conversion.impl.Html2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.Image2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.Pdf2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.PowerPoint2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.Tgs2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.Tiff2AnyConverter;
import ru.gadjini.any2any.service.conversion.impl.Txt2AnyConvert;
import ru.gadjini.any2any.service.conversion.impl.Word2AnyConverter;
import ru.gadjini.any2any.service.image.editor.StateFather;
import ru.gadjini.any2any.service.image.editor.filter.FilterState;
import ru.gadjini.any2any.service.image.editor.transparency.ColorState;
import ru.gadjini.any2any.service.image.resize.ResizeState;
import ru.gadjini.any2any.service.ocr.OcrService;
import ru.gadjini.any2any.service.thumb.ThumbService;
import ru.gadjini.any2any.service.unzip.UnzipService;

import java.util.Set;

public class Constants {

    private Constants() {}

    public static final Set<String> FILE_TAGS = Set.of(
            ConvertMaker.TAG,
            RenameService.RenameTask.TAG,
            ArchiveService.ArchiveTask.TAG,
            Epub2AnyConverter.TAG,
            Excel2AnyConverter.TAG,
            FormatService.TAG,
            Html2AnyConverter.TAG,
            Image2AnyConverter.TAG,
            Pdf2AnyConverter.TAG,
            PowerPoint2AnyConverter.TAG,
            Tgs2AnyConverter.TAG,
            Tiff2AnyConverter.TAG,
            Txt2AnyConvert.TAG,
            Word2AnyConverter.TAG,
            StateFather.TAG,
            OcrService.TAG,
            ThumbService.TAG,
            UnzipService.UnzipTask.TAG,
            FilterState.TAG,
            ColorState.TAG,
            ResizeState.TAG
    );
}
