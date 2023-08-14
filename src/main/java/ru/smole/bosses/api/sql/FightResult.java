package ru.smole.bosses.api.sql;

import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.smole.bosses.api.Bosses;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "fight_results")
@Data
public class FightResult {

    @DatabaseField(columnName = "boss_id")
    private String bossId;

    @DatabaseField(columnName = "when_killed")
    private long whenKilled;

    @DatabaseField
    private String result;

    public void setResult(@NotNull Map<String, Double> result) {
        val limitedResults = result.entrySet()
                .stream()
                .limit(3)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.result = Bosses.GSON.toJson(limitedResults);
    }

    @NotNull
    public Map<String, Double> getResult() {
        Map<String, Double> fightResult = Bosses.GSON.fromJson(result, new TypeToken<Map<String, Double>>() {}.getType());

        if (fightResult == null) {
            fightResult = Collections.emptyMap();
        }

        return fightResult;
    }
}
