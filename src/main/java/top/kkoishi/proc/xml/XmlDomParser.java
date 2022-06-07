package top.kkoishi.proc.xml;

import org.jetbrains.annotations.NotNull;
import top.kkoishi.proc.property.BuildFailedException;
import top.kkoishi.proc.xml.dom.AbstractXmlNode;
import top.kkoishi.proc.xml.dom.XmlComment;
import top.kkoishi.proc.xml.dom.XmlDocTree;
import top.kkoishi.proc.xml.dom.XmlDomKt;
import top.kkoishi.proc.xml.dom.XmlElementInfoDesc;
import top.kkoishi.proc.xml.dom.XmlLeafNode;
import top.kkoishi.proc.xml.dom.XmlNodeImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * @author KKoishi_
 */
public class XmlDomParser extends XmlParser<XmlDocTree> {
    protected char lookForward;
    protected XmlDocTree dom;

    public XmlDomParser (@NotNull Iterator<Character> rest) {
        super(rest);
    }

    public XmlDomParser (@NotNull String xmlContent) {
        super(xmlContent);
    }

    public static void main (String[] args) throws BuildFailedException, XmlSyntaxException, IOException {
        final XmlDomParser xmlDomParser = new XmlDomParser(Files.readString(Path.of("./node.xml")));
        xmlDomParser.parse();
        System.out.println(xmlDomParser.build());
        System.out.println(xmlDomParser.getTokens());
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    protected void jump () throws XmlSyntaxException {
        if (stackEmpty() && !hasMore()) {
            return;
        }
        switch_loop:
        while (true) {
            switch (lookForward) {
                case '<': {
                    if (!hasMore()) {
                        throw new XmlSyntaxException("Meet EOF while Syntax Analysing:There still contains not-completed-element in this xml document!");
                    }
                    switch (this.lookForward = lookForward()) {
                        case '?': {
                            docDesc();
                            break switch_loop;
                        }
                        case '!': {
                            if (!hasMore()) {
                                throw new XmlSyntaxException("Meet EOF while Syntax Analysing:There still contains not-completed-element in this xml document!");
                            }
                            if ((lookForward = lookForward()) == '-') {
                                comment();
                            } else if (lookForward == '[') {
                                ignore();
                            } else {
                                throw new XmlSyntaxException("Error xml token:Expect '<![CDATA[' or '<!--', but got different token.");
                            }
                            break switch_loop;
                        }
                        case '/': {
                            endElement();
                            break;
                        }
                        default: {
                            element();
                            break;
                        }
                    }
                    break switch_loop;
                }
                case ' ', '\r', '\n', '\t': {
                    this.lookForward = lookForward();
                    continue switch_loop;
                }
                default: {
                    text();
                    break switch_loop;
                }
            }
        }
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    protected void text () throws XmlSyntaxException {
        appendChar(lookForward);
        while (hasMore()) {
            this.lookForward = lookForward();
            switch (lookForward) {
                case '<':
                case '\r', '\n': {
                    if (!getBuf().isEmpty()) {
                        addToken(new Token(XmlType.TEXT, getBuf().toString()));
                        clearBuf();
                    }
                    jump();
                    return;
                }
                default: {
                    appendChar(lookForward);
                }
            }
        }
    }

    protected void element () throws XmlSyntaxException {
        super.appendChar(lookForward);
        boolean indexString = false;
        while (hasMore()) {
            this.lookForward = lookForward();
            if (indexString) {
                if (lookForward == '"') {
                    indexString = false;
                }
                appendChar(lookForward);
            } else if (lookForward == '"') {
                indexString = true;
                appendChar('"');
            } else if (lookForward == '/') {
                if (!hasMore()) {
                    throw new XmlSyntaxException("Meet EOF while Syntax Analysing:The final element is not finished!");
                }
                this.lookForward = lookForward();
                if (lookForward == '>') {
                    addToken(new Token(XmlType.ELEMENT, getBuf().toString()));
                    clearBuf();
                    this.lookForward = lookForward();
                    jump();
                    return;
                } else {
                    throw new XmlSyntaxException();
                }
            } else if (lookForward == '>') {
                addToken(new Token(XmlType.LEFT_ELE, getBuf().toString()));
                super.push(getElementName(getBuf().toString()));
                clearBuf();
                this.lookForward = lookForward();
                jump();
                return;
            } else {
                appendChar(lookForward);
            }
        }
    }

    protected String getElementName (String content) {
        final var pos = content.indexOf(' ');
        return pos != -1 ? content.substring(0, pos) : content;
    }

    protected void endElement () throws XmlSyntaxException {
        while (hasMore()) {
            this.lookForward = lookForward();
            if (lookForward == '>') {
                if (super.pop().equals(getBuf().toString())) {
                    addToken(new Token(XmlType.RIGHT_ELE, getBuf().toString()));
                    clearBuf();
                    if (!stackEmpty()) {
                        if (!hasMore()) {
                            throw new XmlSyntaxException();
                        }
                        this.lookForward = lookForward();
                        jump();
                    }
                    return;
                } else {
                    throw new XmlSyntaxException();
                }
            } else {
                appendChar(lookForward);
            }
        }
    }

    /**
     * DOC_DESC:elements like: {@code <?xxx?>}
     */
    protected void docDesc () throws XmlSyntaxException {
        while (hasMore()) {
            this.lookForward = lookForward();
            if (lookForward == '?') {
                this.lookForward = lookForward();
                if (lookForward == '>') {
                    if (!hasMore()) {
                        throw new XmlSyntaxException("Meet EOF while Syntax Analysing:There is no root element in this xml document!");
                    }
                    super.addToken(new Token(XmlType.DOC_DESC, super.getBuf().toString()));
                    super.clearBuf();
                    this.lookForward = lookForward();
                    jump();
                    return;
                } else {
                    super.appendChar(lookForward);
                }
            } else {
                super.appendChar(lookForward);
            }
        }
        throw new XmlSyntaxException("Meet EOF while Syntax Analysing:There is no root element in this xml document!");
    }

    /**
     * Xml comment:{@code <!--xxx-->}
     */
    protected void comment () throws XmlSyntaxException {
        int count = 0;
        if (!hasMore()) {
            throw new XmlSyntaxException("Meet EOF while Syntax Analysing:The xml comment element is not ended!");
        }
        this.lookForward = lookForward();
        if (lookForward == '-') {
            comment_def:
            while (hasMore()) {
                this.lookForward = lookForward();
                if (lookForward == '-') {
                    ++count;
                    this.lookForward = lookForward();
                    while (hasMore()) {
                        if (lookForward == '-') {
                            ++count;
                        } else if (lookForward == '>') {
                            if (count == 2) {
                                addToken(new Token(XmlType.COMMENT, this.getBuf().toString()));
                                this.clearBuf();
                                this.lookForward = lookForward();
                                jump();
                                return;
                            } else {
                                if (count > 2) {
                                    throw new XmlSyntaxException();
                                } else {
                                    count = 0;
                                    this.appendChar('-').append('>');
                                }
                            }
                        } else {
                            if (count <= 1) {
                                count = 0;
                                this.appendChar('-');
                            } else {
                                throw new XmlSyntaxException();
                            }
                            this.appendChar(lookForward);
                            continue comment_def;
                        }
                        this.lookForward = lookForward();
                    }
                } else if (lookForward == '>') {
                    if (count == 2) {
                        addToken(new Token(XmlType.COMMENT, this.getBuf().toString()));
                        this.clearBuf();
                        this.lookForward = lookForward();
                        jump();
                        return;
                    }
                } else {
                    this.appendChar(lookForward);
                }
            }
        }
        throw new XmlSyntaxException();
    }

    /**
     * Ignored by parser element:{@code <![CDATA[xxx]]>}
     */
    protected void ignore () throws XmlSyntaxException {
        for (int i = 0; i < 6; i++) {
            if (!hasMore()) {
                throw new XmlSyntaxException("Meet EOF while Syntax Analysing:The xml ignore element is not completed yet.");
            }
            this.lookForward = lookForward();
            appendChar(lookForward);
        }
        if ("CDATA[".equals(this.getBuf().toString())) {
            //right format
            while (hasMore()) {
                this.lookForward = lookForward();
                if (lookForward == ']') {
                    if (lookForward() != ']') {
                        throw new XmlSyntaxException("Meet EOF while Syntax Analysing:The xml ignore element is not completed yet.");
                    }
                    if (lookForward() != '>') {
                        throw new XmlSyntaxException("Meet EOF while Syntax Analysing:The xml ignore element is not completed yet.");
                    }
                    this.lookForward = lookForward();
                    jump();
                    return;
                }
            }
        }
        throw new XmlSyntaxException("Syntax Error:The format of xml parser-ignore element should be like '<![CDATA[xxx]]>'!");
    }

    @Override
    public void parse () throws XmlSyntaxException, BuildFailedException {
        this.lookForward = super.lookForward();
        jump();
        this.dom = parseTokens();
    }

    @Override
    @NotNull
    public XmlDocTree build () {
        return dom;
    }

    @NotNull
    protected XmlDocTree parseTokens () {
        final XmlDocTree dom = new XmlDocTree();
        final var nodes = build0();
        while (!nodes.isEmpty()) {
            dom.getRoot().children().addLast(nodes.removeFirst());
        }
        return dom;
    }

    @Override
    public void reset (@NotNull Iterator<Character> rest) {
        this.lookForward = '\u0000';
        this.dom = null;
        super.reset(rest);
    }

    protected ArrayDeque<AbstractXmlNode> build0 () {
        if (tokensEmpty()) {
            return new ArrayDeque<>(0);
        }
        final ArrayDeque<AbstractXmlNode> xmlNodes = new ArrayDeque<>(8);
        while (!tokensEmpty()) {
            final var token = removeToken();
            final var value = token.value();
            switch (token.type()) {
                case LEFT_ELE:
                    assert value != null;
                    final XmlNodeImpl node = new XmlNodeImpl(new XmlElementInfoDesc(value));
                    final var children = build0();
                    while (!children.isEmpty()) {
                        node.children().addLast(children.removeFirst());
                    }
                    xmlNodes.addLast(node);
                    break;
                case RIGHT_ELE:
                    return xmlNodes;
                case ELEMENT:
                    assert value != null;
                    xmlNodes.addLast(new XmlLeafNode(new XmlElementInfoDesc(value)));
                    break;
                case TEXT:
                    assert value != null;
                    xmlNodes.addLast(new XmlLeafNode(value));
                    break;
                case COMMENT: {
                    xmlNodes.addLast(new XmlLeafNode(new XmlComment(token.component2())));
                    break;
                }
                case DOC_DESC: {
                    assert value != null;
                    xmlNodes.addLast(new XmlLeafNode(XmlDomKt.cast(new XmlElementInfoDesc(value))));
                    break;
                }
                default:
                    break;
            }
        }
        return xmlNodes;
    }
}
