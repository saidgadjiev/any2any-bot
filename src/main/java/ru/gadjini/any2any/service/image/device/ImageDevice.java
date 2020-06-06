package ru.gadjini.any2any.service.image.device;

public interface ImageDevice {

    void convert(String in, String out, String ... options);

    void negativeTransparent(String in, String out, String... colors);

    void positiveTransparent(String in, String out, String color);
}
