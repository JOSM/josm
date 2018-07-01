// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;

/**
 * Checks that there is Javadoc for every top level class, interface or enum.
 */
public class TopLevelJavadocCheck extends AbstractCheck {

    private boolean foundTopLevelClass;

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.ENUM_DEF};
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[0];
    }

    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        foundTopLevelClass = false;
    }

    @Override
    public void finishTree(DetailAST rootAST) {
        if (!foundTopLevelClass) {
            this.log(rootAST.getLineNo(), "assertion failure: unable to find toplevel class or interface");
        }
    }

    private boolean hasJavadoc(DetailAST ast) {
        DetailAST blockCommentBegin = ast.findFirstToken(TokenTypes.BLOCK_COMMENT_BEGIN);
        if (blockCommentBegin == null) {
            DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
            if (modifiers == null)
                return false;
            blockCommentBegin = modifiers.findFirstToken(TokenTypes.BLOCK_COMMENT_BEGIN);
            if (blockCommentBegin == null) {
                DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
                if (annotation == null)
                    return false;
                blockCommentBegin = annotation.findFirstToken(TokenTypes.BLOCK_COMMENT_BEGIN);
                if (blockCommentBegin == null)
                    return false;
            }
        }
        if (!JavadocUtil.isJavadocComment(blockCommentBegin))
            return false;
        DetailNode javadocTree = new JavadocDetailNodeParser().parseJavadocAsDetailNode(blockCommentBegin).getTree();
        return hasProperText(javadocTree);
    }

    private boolean hasProperText(DetailNode javadoc) {
        if (javadoc == null) return false;
        for (DetailNode child : javadoc.getChildren()) {
            if (child.getType() == JavadocTokenTypes.TEXT) {
                if (!child.getText().trim().isEmpty())
                    return true;
            } else if (child.getType() == JavadocTokenTypes.HTML_ELEMENT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitToken(DetailAST ast) {
        DetailAST parent = ast.getParent();
        if (parent == null || parent.getType() == TokenTypes.EOF) {
            foundTopLevelClass = true;
            if (!hasJavadoc(ast)) {
                this.log(ast.getLineNo(), "incomplete or missing Javadoc for top level class or interface");
            }
        }
   }
}
