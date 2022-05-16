package base;

public class RandomUtil {
    public static int getRandomInteger(int minimum, int maximum) {
        return ((int) (Math.random() * (maximum - minimum))) + minimum;
    }
}
