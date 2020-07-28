package ru.gadjini.any2any.domain;

import ru.gadjini.any2any.model.Any2AnyFile;

public interface HasThumb {

    void setThumb(Any2AnyFile thumb);

    void delThumb();

    Any2AnyFile getThumb();
}
