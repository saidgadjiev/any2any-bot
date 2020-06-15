package ru.gadjini.any2any.service.image.editor.transparency;

import java.util.ArrayList;
import java.util.List;

public enum Color {

    RED {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#F70A38", "#C10A2D", "#AC0727", "#E04563", "#E41A1A", "#B20E0E", "#E6311E", "#A21A0C", "#E36A5E", "#B11B19",
                    "#9B3A39"
            };
        }
    },
    ORANGE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "OrangeRed", "OrangeRed1", "OrangeRed2", "OrangeRed3",
                    "orange", "orange1", "orange2", "orange3", "DarkGoldenrod3", "DarkGoldenrod2", "DarkGoldenrod1",
                    "DarkGoldenrod", "goldenrod", "goldenrod1", "goldenrod2", "goldenrod3", "gold3", "DarkOrange", "DarkOrange1",
                    "DarkOrange2", "DarkOrange3", "chocolate2", "chocolate3", "chocolate1", "chocolate",
                    "sienna2", "sienna1", "#f2ba5f", "#b07512", "#f4b85d"
            };
        }
    },
    YELLOW {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "yellow", "yellow1", "yellow2", "yellow3", "yellow4", "gold", "gold1", "gold2"
            };
        }
    },
    GREEN {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#40F909", "#32A210", "#62B749", "#4B9137", "#1E7206", "#8AE96E", "#64F13A", "#40D513", "#23D717", "#31F54C",
                    "#18B12D", "#6CF17E", "#09F729", "#05A02A", "#4FCF6E", "#35c746", "#1b7c30", "#98efa1"
             };
        }
    },
    BLUE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "blue", "blue1", "blue2", "blue3", "MediumBlue", "blue4", "DarkBlue", "MidnightBlue", "navy", "NavyBlue", "RoyalBlue", "RoyalBlue1", "RoyalBlue2",
                    "RoyalBlue3", "RoyalBlue4", "CornflowerBlue", "DodgerBlue", "DodgerBlue1", "DodgerBlue2", "DodgerBlue3", "DodgerBlue4",
                    "DeepSkyBlue", "DeepSkyBlue1", "DeepSkyBlue2", "DeepSkyBlue3", "DeepSkyBlue4", "SlateBlue4", "SlateBlue3", "SlateBlue2", "SlateBlue1", "SlateBlue",
                    "MediumSlateBlue", "LightSlateBlue", "DarkSlateBlue", "#0630d0", "#004af7", "#0222a4", "#3b00f8", "#2849ff"
            };
        }
    },
    PINK {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "#EA16D3", "#9E2191", "#C43EB6", "#F553E4", "#F392E9", "#B607A4", "#D375CA", "#83317C", "#A25D9C",
                    "#8E0983", "#5F0757", "#D828F0"
            };
        }
    },
    PURPLE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "purple", "indigo", "BlueViolet", "purple2", "purple3", "purple4", "purple1",
                    "MediumOrchid", "MediumOrchid2", "MediumOrchid3", "fuchsia", "magenta", "magenta1", "magenta2", "magenta3",
                    "DarkViolet", "#cb15ef", "#893f9c", "#b400fa", "DarkMagenta", "magenta4"
            };
        }
    },
    BLACK {
        @Override
        public String[] transparentColors() {
            List<String> colors = new ArrayList<>(List.of("black", "opaque"));
            for (int i = 24; i > -1; --i) {
                colors.add("grey" + i);
                colors.add("gray" + i);
            }

            return colors.toArray(new String[0]);
        }
    },
    WHITE {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "white", "gray100", "gray100", "gray99", "gray99", "gray98", "gray98", "seashell", "seashell1", "FloralWhite", "OldLace", "ivory", "ivory1",
                    "honeydew", "honeydew1", "MintCream", "azure", "azure1", "AliceBlue", "GhostWhite", "LavenderBlush", "LavenderBlush1",
                    "snow", "snow1"
            };
        }
    },
    GRAY {
        @Override
        public String[] transparentColors() {
            List<String> colors = new ArrayList<>(List.of(
                    "snow4", "LavenderBlush4", "plum3", "thistle4", "snow3", "LavenderBlush3", "LemonChiffon3", "cornsilk3", "PeachPuff3", "thistle3",
                    "plum4", "LightSteelBlue4", "SlateGray4", "LightSlateGray", "LightSlateGrey", "SlateGray", "SlateGrey", "SlateGray3",
                    "SkyBlue4", "LightSkyBlue4", "LightSkyBlue3", "LightBlue3", "LightBlue4", "CadetBlue4", "azure4", "LightCyan4", "PaleTurquoise4",
                    "DarkSlateGray", "DarkSlateGrey", "aquamarine4", "honeydew4", "honeydew3", "DarkSeaGreen4",
                    "ivory4", "LightYellow4", "LemonChiffon4", "cornsilk4", "wheat3", "wheat4", "AntiqueWhite4",
                    "bisque4", "AntiqueWhite3", "bisque3", "seashell4", "MistyRose3", "MistyRose4", "DarkGray", "DarkGrey",
                    "fractal", "gray", "DimGray", "DimGrey", "silver", "SlateGray2", "ivory3", "LemonChiffon3", "cornsilk3", "PeachPuff3", "seashell3"
            ));
            for (int i = 81; i > 24; --i) {
                colors.add("grey" + i);
                colors.add("gray" + i);
            }

            return colors.toArray(new String[0]);
        }
    },
    BROWN {
        @Override
        public String[] transparentColors() {
            return new String[]{
                    "firebrick", "RosyBrown4", "brown", "brown2", "brown3", "brown",
                    "brown4", "IndianRed4", "khaki4", "gold4", "goldenrod4", "DarkGoldenrod4", "orange4", "DarkOrange4", "burlywood4", "NavajoWhite4",
                    "tan4", "chocolate4", "SaddleBrown", "sienna4",
                    "OrangeRed4", "coral4", "tomato4", "#743b17"
            };
        }
    };

    public abstract String[] transparentColors();
}
