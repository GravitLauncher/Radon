/*
 * Copyright (C) 2018 ItzSomebody
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.itzsomebody.radon.config;

import me.itzsomebody.radon.Dictionaries;
import me.itzsomebody.radon.SessionInfo;
import me.itzsomebody.radon.exceptions.IllegalConfigurationValueException;
import me.itzsomebody.radon.exceptions.RadonException;
import me.itzsomebody.radon.exclusions.Exclusion;
import me.itzsomebody.radon.exclusions.ExclusionManager;
import me.itzsomebody.radon.transformers.Transformer;
import me.itzsomebody.radon.transformers.miscellaneous.Crasher;
import me.itzsomebody.radon.transformers.miscellaneous.watermarker.Watermarker;
import me.itzsomebody.radon.transformers.miscellaneous.watermarker.WatermarkerSetup;
import me.itzsomebody.radon.transformers.obfuscators.flow.FlowObfuscation;
import me.itzsomebody.radon.transformers.obfuscators.invokedynamic.InvokeDynamic;
import me.itzsomebody.radon.transformers.obfuscators.miscellaneous.HideCode;
import me.itzsomebody.radon.transformers.obfuscators.miscellaneous.MemberShuffler;
import me.itzsomebody.radon.transformers.obfuscators.numbers.NumberObfuscation;
import me.itzsomebody.radon.transformers.obfuscators.strings.StringEncryption;
import me.itzsomebody.radon.transformers.obfuscators.strings.StringEncryptionSetup;
import me.itzsomebody.radon.transformers.obfuscators.strings.StringPool;
import me.itzsomebody.radon.transformers.optimizers.Optimizer;
import me.itzsomebody.radon.transformers.optimizers.OptimizerDelegator;
import me.itzsomebody.radon.transformers.optimizers.OptimizerSetup;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * A big mess which somehow parses the configuration files.
 *
 * @author ItzSomebody
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConfigurationParser {
    private Map<String, Object> map;
    private final static Set<String> VALID_KEYS = new HashSet<>();

    static {
        for (ConfigurationSetting setting : ConfigurationSetting.values())
            VALID_KEYS.add(setting.getValue());
    }

    public ConfigurationParser(InputStream in) {
        this.map = new Yaml().load(in);
        this.map.keySet().forEach(s -> {
            if (!VALID_KEYS.contains(s))
                throw new RadonException(s + " is not a valid configuration setting.");
        });
    }

    public SessionInfo createSessionFromConfig() {
        SessionInfo info = new SessionInfo();
        info.setTransformers(getTransformers());
        info.setExclusions(getExclusions());
        info.setTrashClasses(getTrashClasses());
        info.setDictionaryType(getDictionary());
        return info;
    }

    private List<Transformer> getTransformers() {
        ArrayList<Transformer> transformers = new ArrayList<>();
        transformers.add(getOptimizerTransformer());
        transformers.add(getNumberObfuscationTransformer());
        Transformer trans = getInvokeDynamicTransformer();
        if (trans != null) transformers.add(trans);
        List<StringEncryption> stringEncrypters = getStringEncryptionTransformers();
        if (stringEncrypters != null) {
            transformers.addAll(stringEncrypters);
        }
        transformers.add(getFlowObfuscationTransformer());
        transformers.add(getShufflerTransformer());
        transformers.add(getCrasherTransformer());
        transformers.add(getHideCodeTransformer());
        transformers.add(getWatermarkerTransformer());

        return transformers;
    }

    private Optimizer getOptimizerTransformer() {
        Object o = map.get(ConfigurationSetting.OPTIMIZER.getValue());
        if (o == null)
            return null;
        if (!(o instanceof Map))
            throw new IllegalConfigurationValueException(ConfigurationSetting.OPTIMIZER.getValue(), Map.class,
                    o.getClass());

        try {
            Map<String, Boolean> optimizerSettings = (Map) o;
            if (!optimizerSettings.get("Enabled"))
                return null;

            boolean gotoGoto = optimizerSettings.getOrDefault("InlineGotoGoto", false);
            boolean gotoReturn = optimizerSettings.getOrDefault("InlineGotoReturn", false);
            boolean nopInstructions = optimizerSettings.getOrDefault("RemoveNopInstructions", false);

            return new OptimizerDelegator(new OptimizerSetup(nopInstructions, gotoGoto, gotoReturn));
        } catch (ClassCastException e) {
            throw new IllegalConfigurationValueException("Error while parsing optimizer setup: " + e.getMessage());
        }
    }

    private NumberObfuscation getNumberObfuscationTransformer() {
        Object o = map.get(ConfigurationSetting.NUMBER_OBFUSCATION.getValue());
        if (o == null)
            return null;
        if (!(o instanceof String))
            throw new IllegalConfigurationValueException(ConfigurationSetting.NUMBER_OBFUSCATION.getValue(),
                    String.class, o.getClass());

        String s = (String) o;
        if (!"Light".equals(s) && !"Normal".equals(s) && !"Heavy".equals(s))
            throw new IllegalConfigurationValueException("Expected Light, Normal or Heavy as mode for number " +
                    "obfuscation. Got " + s + " instead.");

        return NumberObfuscation.getTransformerFromString(s);
    }

    private InvokeDynamic getInvokeDynamicTransformer() {
        Object o = map.get(ConfigurationSetting.INVOKEDYNAMIC.getValue());
        if (o == null)
            return null;
        if (!(o instanceof String))
            throw new IllegalConfigurationValueException(ConfigurationSetting.INVOKEDYNAMIC.getValue(), String.class,
                    o.getClass());
        String s = (String) o;
        if (s.equalsIgnoreCase("none")) return null;
        if (!"Light".equals(s) && !"Normal".equals(s) && !"Heavy".equals(s))
            throw new IllegalConfigurationValueException("Expected Light, Normal or Heavy as mode for invokedynamic " +
                    "obfuscation. Got " + s + " instead.");


        return InvokeDynamic.getTransformerFromString(s);
    }

	private List<StringEncryption> getStringEncryptionTransformers() {
        Object o = map.get(ConfigurationSetting.STRING_ENCRYPTION.getValue());
        if (o == null)
            return null;
        if (!(o instanceof Map))
            throw new IllegalConfigurationValueException(ConfigurationSetting.STRING_ENCRYPTION.getValue(), Map.class,
                    o.getClass());
		Map<String, Object> settings = (Map) o;
        if (!(boolean) settings.get("Enabled"))
            return null;

        String s = (String) settings.get("Mode");
        if (!"Light".equals(s) && !"Normal".equals(s) && !"Heavy".equals(s))
            throw new IllegalConfigurationValueException("Expected Light, Normal or Heavy as mode for string " +
                    "encryption. Got " + s + " instead.");

        boolean pool = (boolean) settings.getOrDefault("StringPool", false);
        List<String> exclusions = (List) settings.getOrDefault("Exclusions", new ArrayList<String>());

        ArrayList<StringEncryption> things = new ArrayList<>();
        things.add(StringEncryption.getTransformerFromString(s, new StringEncryptionSetup(exclusions)));
        if (pool)
            things.add(new StringPool(new StringEncryptionSetup(exclusions)));

        return things;
    }

    private FlowObfuscation getFlowObfuscationTransformer() {
        Object o = map.get(ConfigurationSetting.FLOW_OBFUSCATION.getValue());
        if (o == null)
            return null;
        if (!(o instanceof String))
            throw new IllegalConfigurationValueException(ConfigurationSetting.FLOW_OBFUSCATION.getValue(),
                    String.class, o.getClass());

        String s = (String) o;
        if (!"Light".equals(s) && !"Normal".equals(s) && !"Heavy".equals(s))
            throw new IllegalConfigurationValueException("Expected Light, Normal or Heavy as mode for flow " +
                    "obfuscation. Got " + s + " instead.");


        return FlowObfuscation.getTransformerFromString(s);
    }

    private MemberShuffler getShufflerTransformer() {
        Object o = map.get(ConfigurationSetting.SHUFFLER.getValue());
        if (o == null)
            return null;
        if (!(o instanceof Boolean))
            throw new IllegalConfigurationValueException(ConfigurationSetting.SHUFFLER.getValue(), Boolean.class,
                    o.getClass());


        return ((boolean) o) ? new MemberShuffler() : null;
    }

    private Crasher getCrasherTransformer() {
        Object o = map.get(ConfigurationSetting.CRASHER.getValue());
        if (o == null)
            return null;
        if (!(o instanceof Boolean))
            throw new IllegalConfigurationValueException(ConfigurationSetting.CRASHER.getValue(), Boolean.class,
                    o.getClass());

        return ((Boolean) o) ? new Crasher() : null;
    }

    private HideCode getHideCodeTransformer() {
        Object o = map.get(ConfigurationSetting.HIDE_CODE.getValue());
        if (o == null)
            return null;
        if (!(o instanceof Boolean))
            throw new IllegalConfigurationValueException(ConfigurationSetting.HIDE_CODE.getValue(), Boolean.class,
                    o.getClass());

        return ((Boolean) o) ? new HideCode() : null;
    }

    private Watermarker getWatermarkerTransformer() {
        Object o = map.get(ConfigurationSetting.WATERMARK.getValue());
        if (o == null)
            return null;
        if (!(o instanceof Map))
            throw new IllegalConfigurationValueException(ConfigurationSetting.WATERMARK.getValue(), Map.class,
                    o.getClass());

        try {
            Map<String, Object> settings = (Map) o;
            if (!(Boolean) settings.get("Enabled"))
                return null;

            String message = (String) settings.get("Message");
            String key = (String) settings.get("Key");

            return new Watermarker(new WatermarkerSetup(message, key));
        } catch (ClassCastException e) {
            throw new IllegalConfigurationValueException("Error while parsing watermark setup: " + e.getMessage());
        }
    }

    private ExclusionManager getExclusions() {
        ExclusionManager exclusions = new ExclusionManager();
        Object o = map.get(ConfigurationSetting.EXCLUSIONS.getValue());
        if (o == null)
            return exclusions;
        if (!(o instanceof List))
            throw new IllegalConfigurationValueException(ConfigurationSetting.EXCLUSIONS.getValue(), List.class,
                    o.getClass());

        try {
            List<String> list = (List) o;
            list.forEach(s -> exclusions.addExclusion(new Exclusion(s)));
        } catch (ClassCastException e) {
            throw new IllegalConfigurationValueException("Error while parsing exclusion setup: " + e.getMessage());
        }

        return exclusions;
    }

    private int getTrashClasses() {
        Object o = map.get(ConfigurationSetting.TRASH_CLASSES.getValue());
        if (o == null)
            return -1;
        if (!(o instanceof Integer))
            throw new IllegalConfigurationValueException(ConfigurationSetting.TRASH_CLASSES.getValue(), Integer.class,
                    o.getClass());
        return (int) o;
    }

    private Dictionaries getDictionary() {
        Object o = map.get(ConfigurationSetting.DICTIONARY.getValue());
        if (o == null)
            return Dictionaries.ALPHABETICAL;
        if (!(o instanceof String) && !(o instanceof Integer))
            throw new IllegalConfigurationValueException(ConfigurationSetting.DICTIONARY.getValue(), String.class,
                    o.getClass());

        if (o instanceof String)
            return Dictionaries.stringToDictionary((String) o);
        else
            return Dictionaries.intToDictionary((int) o);
    }
}
