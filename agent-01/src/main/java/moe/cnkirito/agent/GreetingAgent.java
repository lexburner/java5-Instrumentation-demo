package moe.cnkirito.agent;

import java.lang.instrument.Instrumentation;

public class GreetingAgent {
    public static void premain(String options, Instrumentation ins) {
        if (options != null) {
            System.out.printf("  I've been called with options: \"%s\"\n", options);
        }
        else
            System.out.println("  I've been called with no options.");
        ins.addTransformer(new GreetingTransformer());
    }
}
