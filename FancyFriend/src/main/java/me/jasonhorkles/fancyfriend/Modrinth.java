package me.jasonhorkles.fancyfriend;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class Modrinth {
    public OptionData getProjects() {
        JSONArray input;
        OptionData errorOption = new OptionData(OptionType.STRING,
            "error",
            "Unable to get plugin list",
            false);
        try {
            InputStream url = new URI("https://api.modrinth.com/v2/user/YADN8hKN/projects").toURL()
                .openStream();
            input = new JSONArray(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();
        } catch (Exception e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
            return errorOption;
        }

        Collection<Command.Choice> choices = new ArrayList<>();
        for (Object project : input) {
            JSONObject projectJson = new JSONObject(project.toString());
            choices.add(new Command.Choice(projectJson.getString("title"), projectJson.getString("id")));
        }

        return choices.isEmpty() ? errorOption : new OptionData(OptionType.STRING,
            "plugin",
            "The plugin",
            true).addChoices(choices.stream().sorted(Comparator.comparing(Command.Choice::getName)).toList());
    }

    @Nullable
    public JSONObject getProjectInfo(String projectId) {
        JSONObject input;
        try {
            InputStream url = new URI("https://api.modrinth.com/v2/project/" + projectId).toURL()
                .openStream();
            input = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();
        } catch (Exception e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
            return null;
        }

        return input;
    }
}
