package ru.overwrite.ublocker.blockgroups;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class GroupUtils {

    public Set<String> createStringSet(List<String> input) {
        Set<String> set = new ObjectOpenHashSet<>(input.size());
        for (String s : input) {
            set.add(s.toLowerCase());
        }
        return set;
    }

    public Set<Pattern> createPatternSet(List<String> input) {
        Set<Pattern> set = new ObjectOpenHashSet<>(input.size());
        for (String s : input) {
            set.add(Pattern.compile(s));
        }
        return set;
    }

    public List<String> createStringList(List<String> input) {
        List<String> list = new ObjectArrayList<>(input.size());
        for (String s : input) {
            list.add(s.toLowerCase());
        }
        return list;
    }

    public List<Pattern> createPatternList(List<String> input) {
        List<Pattern> list = new ObjectArrayList<>(input.size());
        for (String s : input) {
            list.add(Pattern.compile(s));
        }
        return list;
    }
}
