package com.energyxxer.trident.global.temp;

import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.crossbow.compiler.CrossbowCompiler;
import com.energyxxer.crossbow.compiler.lexer.CrossbowLexerProfile;
import com.energyxxer.enxlex.lexical_analysis.EagerLexer;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenPatternMatch;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.nbtmapper.parser.NBTTMLexerProfile;
import com.energyxxer.nbtmapper.parser.NBTTMProductions;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.syntaxlang.TDNMetaLexerProfile;
import com.energyxxer.trident.compiler.lexer.syntaxlang.TDNMetaProductions;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.temp.lang_defaults.parsing.MCFunctionProductions;
import com.energyxxer.trident.global.temp.lang_defaults.presets.JSONLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.MCFunctionLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.PropertiesLexerProfile;
import com.energyxxer.trident.ui.dialogs.settings.SnippetLexerProfile;
import com.energyxxer.util.Factory;

import java.io.File;
import java.util.*;

/**
 * Created by User on 2/9/2017.
 */
public class Lang {
    private static final ArrayList<Lang> registeredLanguages = new ArrayList<>();

    public static final Lang JSON = new Lang("JSON",
            JSONLexerProfile::new,
            "json", "mcmeta", TridentCompiler.PROJECT_FILE_NAME.substring(1), CrossbowCompiler.PROJECT_FILE_NAME.substring(1)
    );
    public static final Lang PROPERTIES = new Lang("PROPERTIES",
            PropertiesLexerProfile::new,
            "properties", "lang"
    ) {{this.putProperty("line_comment_marker","#");}};
    public static final Lang MCFUNCTION = new Lang("MCFUNCTION",
            MCFunctionLexerProfile::new,
            () -> MCFunctionProductions.FILE,
            "mcfunction"
    ) {{this.putProperty("line_comment_marker","#");}};
    public static final Lang TRIDENT = new Lang("TRIDENT",
            TridentLexerProfile.INSTANCE::getValue,
            Commons::getActiveTridentProductions,
            "tdn"
    ) {{this.putProperty("line_comment_marker","#");}};
    public static final Lang TRIDENT_META = new Lang("TRIDENT_META",
            TDNMetaLexerProfile::new,
            () -> TDNMetaProductions.FILE,
            "tdnmeta"
    ) {{this.putProperty("line_comment_marker","//");}};
    public static final Lang CROSSBOW = new Lang("CROSSBOW",
            CrossbowLexerProfile.INSTANCE::getValue,
            Commons::getActiveCrossbowProductions,
            "cbw"
    ) {{this.putProperty("line_comment_marker","#");}};
    public static final Lang NBTTM = new Lang("NBTTM",
            () -> new NBTTMLexerProfile(StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT),
            () -> NBTTMProductions.FILE,
            "nbttm"
    ) {{this.putProperty("line_comment_marker","#");}};
    public static final Lang SNIPPET = new Lang("SNIPPET", SnippetLexerProfile::new);

    private final String name;
    private final Factory<LexerProfile> factory;
    private final Factory<GeneralTokenPatternMatch> parserProduction;
    private final List<String> extensions;
    private final HashMap<String, String> properties = new HashMap<>();

    Lang(String name, Factory<LexerProfile> factory, String... extensions) {
        this(name, factory, null, extensions);
    }

    Lang(String name, Factory<LexerProfile> factory, Factory<GeneralTokenPatternMatch> parserProduction, String... extensions) {
        this.name = name;
        this.factory = factory;
        this.parserProduction = parserProduction;
        this.extensions = Arrays.asList(extensions);

        registeredLanguages.add(this);
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public LexerProfile createProfile() {
        return factory.createInstance();
    }

    public Factory<GeneralTokenPatternMatch> getParserProduction() {
        return parserProduction;
    }

    public static Lang getLangForFile(String path) {
        if(path == null) return null;
        for(Lang lang : Lang.values()) {
            for(String extension : lang.extensions) {
                if(path.endsWith("." + extension)) {
                    return lang;
                }
            }
        }
        return null;
    }

    public LangAnalysisResponse analyze(File file, String text, SuggestionModule suggestionModule, SummaryModule summaryModule) {
        GeneralTokenPatternMatch patternMatch = (parserProduction != null) ? parserProduction.createInstance() : null;

        Lexer lexer;
        TokenMatchResponse response = null;
        ArrayList<Notice> notices = new ArrayList<>();
        ArrayList<Token> tokens;

        if(patternMatch instanceof LazyTokenPatternMatch) {
            lexer = new LazyLexer(new TokenStream(true), (LazyTokenPatternMatch) patternMatch);
            lexer.setSummaryModule(summaryModule);
            if(suggestionModule != null) {
                lexer.setSuggestionModule(suggestionModule);
            }
            ((LazyLexer)lexer).tokenizeParse(file, text, createProfile());
            notices.addAll(lexer.getNotices());

            tokens = new ArrayList<>(lexer.getStream().tokens);
            tokens.remove(0);

            response = ((LazyLexer) lexer).getMatchResponse();
        } else {
            lexer = new EagerLexer(new TokenStream(true));
            lexer.setSummaryModule(summaryModule);
            if(suggestionModule != null) {
                lexer.setSuggestionModule(suggestionModule);
            }
            ((EagerLexer)lexer).tokenize(file, text, createProfile());
            notices.addAll(lexer.getNotices());

            tokens = new ArrayList<>(lexer.getStream().tokens);
            tokens.remove(0);
            tokens.removeIf(token -> !token.type.isSignificant());

            if(patternMatch != null) {

                response = ((TokenPatternMatch) patternMatch).match(tokens);

                if(response != null && !response.matched) {
                    notices.add(new Notice(NoticeType.ERROR, response.getErrorMessage(), response.faultyToken));
                }
            }
        }

        return new LangAnalysisResponse(lexer, response, lexer.getStream().tokens, notices);
    }

    @Override
    public String toString() {
        return name;
    }

    public static Collection<Lang> values() {
        return registeredLanguages;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public void putProperty(String key, String value) {
        properties.put(key, value);
    }

    public static class LangAnalysisResponse {
        public Lexer lexer;
        public TokenMatchResponse response;
        public ArrayList<Token> tokens;
        public ArrayList<Notice> notices;

        public LangAnalysisResponse(Lexer lexer, TokenMatchResponse response, ArrayList<Token> tokens, ArrayList<Notice> notices) {
            this.lexer = lexer;
            this.response = response;
            this.tokens = tokens;
            this.notices = notices;
        }
    }
}