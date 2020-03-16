package org.lwjgl;

import deep.learning.human.internal.DrawStuff;
import deep.learning.human.internal.DrawStuffApi;

public class TestDrawStuff {
    public static void main(String[] args) {
        DrawStuffApi api = new DrawStuffFromScratch();
        DrawStuff.dsFunctions f = new DrawStuff.dsFunctions() {
            @Override
            public void start() {

            }

            @Override
            public void step(boolean pause) {

            }

            @Override
            public void command(char cmd) {

            }

            @Override
            public void stop() {

            }
        };
        api.dsSimulationLoop(new String[]{""}, 200, 100, f);
    }
}
