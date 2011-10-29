// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;


import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.tools.template_engine.Tokenizer.Token;
import org.openstreetmap.josm.tools.template_engine.Tokenizer.TokenType;


public class TemplateParser {
    private final Tokenizer tokenizer;

    private static final Collection<TokenType> EXPRESSION_END_TOKENS = Arrays.asList(TokenType.EOF);
    private static final Collection<TokenType> CONDITION_WITH_APOSTROPHES_END_TOKENS = Arrays.asList(TokenType.APOSTROPHE);

    public TemplateParser(String template) {
        this.tokenizer = new Tokenizer(template);
    }

    private Token check(TokenType expectedToken) throws ParseError {
        Token token = tokenizer.nextToken();
        if (token.getType() != expectedToken)
            throw new ParseError(token, expectedToken);
        else
            return token;
    }

    public TemplateEntry parse() throws ParseError {
        return parseExpression(EXPRESSION_END_TOKENS);
    }

    private TemplateEntry parseExpression(Collection<TokenType> endTokens) throws ParseError {
        List<TemplateEntry> entries = new ArrayList<TemplateEntry>();
        while (true) {
            TemplateEntry templateEntry;
            Token token = tokenizer.lookAhead();
            if (token.getType() == TokenType.CONDITION_START) {
                templateEntry = parseCondition();
            } else if (token.getType() == TokenType.CONTEXT_SWITCH_START) {
                templateEntry = parseContextSwitch();
            } else if (token.getType() == TokenType.VARIABLE_START) {
                templateEntry = parseVariable();
            } else if (endTokens.contains(token.getType()))
                return CompoundTemplateEntry.fromArray(entries.toArray(new TemplateEntry[entries.size()]));
            else if (token.getType() == TokenType.TEXT) {
                tokenizer.nextToken();
                templateEntry = new StaticText(token.getText());
            } else
                throw new ParseError(token);
            entries.add(templateEntry);
        }
    }

    private TemplateEntry parseVariable() throws ParseError {
        check(TokenType.VARIABLE_START);
        String variableName = check(TokenType.TEXT).getText();
        check(TokenType.END);

        return new Variable(variableName);
    }

    private void skipWhitespace() throws ParseError {
        Token token = tokenizer.lookAhead();
        if (token.getType() == TokenType.TEXT && token.getText().trim().isEmpty()) {
            tokenizer.nextToken();
        }
    }

    private TemplateEntry parseCondition() throws ParseError {
        check(TokenType.CONDITION_START);
        Condition result = new Condition();
        while (true) {

            TemplateEntry condition;
            Token searchExpression = tokenizer.skip('\'');
            check(TokenType.APOSTROPHE);
            condition = parseExpression(CONDITION_WITH_APOSTROPHES_END_TOKENS);
            check(TokenType.APOSTROPHE);
            if (searchExpression.getText().trim().isEmpty()) {
                result.getEntries().add(condition);
            } else {
                try {
                    result.getEntries().add(new SearchExpressionCondition(SearchCompiler.compile(searchExpression.getText(), false, false), condition));
                } catch (org.openstreetmap.josm.actions.search.SearchCompiler.ParseError e) {
                    throw new ParseError(searchExpression.getPosition(), e);
                }
            }
            skipWhitespace();

            Token token = tokenizer.lookAhead();
            if (token.getType()  == TokenType.END) {
                tokenizer.nextToken();
                return result;
            } else {
                check(TokenType.PIPE);
            }
        }
    }

    private TemplateEntry parseContextSwitch() throws ParseError {

        check(TokenType.CONTEXT_SWITCH_START);
        Token searchExpression = tokenizer.skip('\'');
        check(TokenType.APOSTROPHE);
        TemplateEntry template = parseExpression(CONDITION_WITH_APOSTROPHES_END_TOKENS);
        check(TokenType.APOSTROPHE);
        ContextSwitchTemplate result;
        if (searchExpression.getText().trim().isEmpty())
            throw new ParseError(tr("Expected search expression"));
        else {
            try {
                Match match = SearchCompiler.compile(searchExpression.getText(), false, false);
                result = new ContextSwitchTemplate(match, template, searchExpression.getPosition());
            } catch (org.openstreetmap.josm.actions.search.SearchCompiler.ParseError e) {
                throw new ParseError(searchExpression.getPosition(), e);
            }
        }
        skipWhitespace();
        check(TokenType.END);
        return result;
    }

}
