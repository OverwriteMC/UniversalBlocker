package ru.overwrite.ublocker.blockgroups;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class GroupUtils {

    public ObjectSet<String> createStringSet(List<String> input) {
        ObjectSet<String> set = new ObjectOpenHashSet<>(input.size());
        for (String s : input) {
            set.add(s.toLowerCase());
        }
        return set;
    }

    public ObjectSet<Pattern> createPatternSet(List<String> input) {
        ObjectSet<Pattern> set = new ObjectOpenHashSet<>(input.size());
        for (String s : input) {
            set.add(Pattern.compile(s));
        }
        return set;
    }

    public ObjectList<String> createStringList(List<String> input) {
        ObjectList<String> list = new ObjectArrayList<>(input.size());
        for (String s : input) {
            list.add(s.toLowerCase());
        }
        return list;
    }

    public ObjectList<Pattern> createPatternList(List<String> input) {
        ObjectList<Pattern> list = new ObjectArrayList<>(input.size());
        for (String s : input) {
            list.add(Pattern.compile(s));
        }
        return list;
    }
}
