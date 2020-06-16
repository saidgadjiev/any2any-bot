package ru.gadjini.any2any.service.image.editor.transparency;

public enum Color {

    RED {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#FF0000"
            };
        }
    },
    ORANGE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#FFA500"
            };
        }
    },
    YELLOW {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#FFFF00"
            };
        }
    },
    GREEN {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#008000", "#00FF00", "#228B22"
             };
        }
    },
    BLUE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#0000FF"
            };
        }
    },
    PINK {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#FF1493", "#FFC0CB", "#FF69B4"
            };
        }
    },
    PURPLE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#800080", "#FF00FF"
            };
        }
    },
    BLACK {
        @Override
        public String[] transparentColors() {
            return new String[] {
                    "#000000"
            };
        }
    },
    WHITE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#FFFFFF"
            };
        }
    },
    BROWN {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#A52A2A"
            };
        }
    };

    public abstract String[] transparentColors();
}
