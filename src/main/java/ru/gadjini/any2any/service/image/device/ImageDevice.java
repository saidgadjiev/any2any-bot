package ru.gadjini.any2any.service.image.device;

public interface ImageDevice {

    void convert(String in, String out, String ... options);

    void transparent(String in, String out, String color, boolean remove);
}
