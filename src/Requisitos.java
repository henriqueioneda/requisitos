import com.google.gson.*;
import com.triptheone.joda.Stopwatch;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by henriqueioneda on 05/04/17.
 */
public class Requisitos {
    class Transition {
        String from;
        String to;
        String chance;
    }

    static class State {
        String name; double probability; boolean visited;
        State(String name, double probability, boolean visited) {
            this.name = name;
            this.probability = probability;
            this.visited = visited;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Estado ");
            sb.append(String.format("%3s", this.name));
            sb.append(": ");
            sb.append(String.format("%13.10f", this.probability * 100));
            sb.append(" %");
            return sb.toString();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        Stopwatch s = Stopwatch.start();
        Gson gson = new Gson();
        FileReader reader = new FileReader("./src/transitions.json");
        List<Transition> transitions = Arrays.asList(gson.fromJson(reader, Transition[].class));
        List<State> states = statesFromTransitions(transitions);
        ScriptEngine engine = startScriptEvalEngine();
        for (int i = 0; i < 10000; i++) {
            executeBfs(states, transitions, engine);
            states.forEach(state -> state.visited = false);
        }
        states.forEach(System.out::println);
        System.out.printf("Tempo de simulação: %s ms", s.getElapsedTime().getMillis());
    }

    private static void executeBfs(List<State> states, List<Transition> transitions, ScriptEngine engine) {
        List<State> previousStates = new ArrayList<>(states);
        Queue<State> queue = new LinkedList<>();
        State initialState = states.stream().filter(state -> Double.compare(state.probability, 1.0) == -1).findFirst().get();
        queue.add(initialState);
        while (!queue.isEmpty()) {
            State currentState = queue.remove();
            transitions.stream()
                .filter(transition -> transition.from.equals(currentState.name))
                .forEach(transition -> {
                    State neighbor = states.stream().filter(state -> state.name.equals(transition.to)).findFirst().get();
                    State oldNeighbor = previousStates.stream().filter(state -> state.name.equals(neighbor.name)).findFirst().get();
                    try {
                        double value = (double) engine.eval(transition.chance) * currentState.probability;
                        neighbor.probability = oldNeighbor.probability + value;
                        currentState.probability -= value;
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }
                    if (!neighbor.visited)
                        queue.add(neighbor);
                });
            currentState.visited = true;
        }
    }

    private static ScriptEngine startScriptEvalEngine() {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.put("l", 1.0/7000);
        engine.put("m", 1.0/1.5);
        engine.put("t", 1.0/60);
        return engine;
    }

    private static List<State> statesFromTransitions(List<Transition> transitions) {
        return transitions.stream()
                .map(transition -> transition.from)
                .distinct()
                .map(name -> {
                    State state = new State(name, 0.0, false);
                    if (name.equals("3OK"))
                        state.probability = 1.0;
                    return state;
                })
                .collect(Collectors.toList());
    }
}