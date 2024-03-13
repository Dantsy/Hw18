package ru.otus;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class RandomNumberHelper {

    public static int getRandomId() {
        return getRandomNumber(1000);
    }

    public static int getRandomNumber(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}