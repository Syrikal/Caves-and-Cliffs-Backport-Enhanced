package syric.speleogenesis.util;

import java.util.Random;

public class RandomGenerators {
    private static final Random rdm = new Random();

    //Pattern for reference:
//    for (int i = 1; i <= output; i++) {
//        doStuff(pos.above(i));
//    }


    //Gives the number of blocks below a ceiling moss block that turn adjacent walls to moss
    //0-3, 20/40/30/10
    public static int MossCeilingDownAdj() {
        double d = rdm.nextDouble();
        if (d < .2) {
            return 0;
        } else if (d < .6) {
            return 1;
        } else if (d < .9) {
            return 2;
        } else {
            return 3;
        }
    }

    //Gives the number of wall blocks above a ceiling moss block that get turned to moss
    //0-3, 10/40/40/10
    public static int MossCeilingUp() {
        double d = rdm.nextDouble();
        if (d < .1) {
            return 0;
        } else if (d < .5) {
            return 1;
        } else if (d < .9) {
            return 2;
        } else {
            return 3;
        }
    }

    //Gives the number of blocks above a clay block that turn adjacent walls to clay
    //0-2, 35/55/10
    public static int ClayUpAdj() {
        double d = rdm.nextDouble();
        if (d < .35) {
            return 0;
        } else if (d < .9) {
            return 1;
        } else {
            return 2;
        }
    }

    //Gives the number of wall blocks above a clay block that get turned to clay
    //0-2, 20/50/30
    public static int ClayUp() {
        double d = rdm.nextDouble();
        if (d < .2) {
            return 0;
        } else if (d < .7) {
            return 1;
        } else {
            return 3;
        }
    }

    //Gives the number of wall blocks below a clay block that get turned to clay
    //0-3, 20/40/30/10
    public static int ClayDown() {
        double d = rdm.nextDouble();
        if (d < .2) {
            return 0;
        } else if (d < .6) {
            return 1;
        } else if (d < .9) {
            return 2;
        } else {
            return 3;
        }
    }

    //Gives the number of wall blocks below a moss block that get turned to moss
    //0-3, 20/40/30/10
    public static int MossDown() {
        double d = rdm.nextDouble();
        if (d < .2) {
            return 0;
        } else if (d < .6) {
            return 1;
        } else if (d < .9) {
            return 2;
        } else {
            return 3;
        }
    }

    //Gives the number of blocks above a floor moss block that turn adjacent walls to moss
    //0-2, 35/55/10
    public static int MossFloorUpAdj() {
        double d = rdm.nextDouble();
        if (d < .35) {
            return 0;
        } else if (d < .9) {
            return 1;
        } else {
            return 3;
        }
    }

    //Gives whether a water block should be replaced with clay
    public static boolean replaceWaterBoolean(int adjacent) {
        if (adjacent > 4) {
            return true;
        } else if (adjacent < 2) {
            return false;
        } else if (adjacent == 2) {
            return rdm.nextDouble() < 0.1;
        } else if (adjacent == 3) {
            return rdm.nextDouble() < 0.25;
        } else {
            return rdm.nextDouble() < 0.5;
        }
    }

}
