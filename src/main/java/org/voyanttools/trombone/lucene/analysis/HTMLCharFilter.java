package org.voyanttools.trombone.lucene.analysis;

/*
 * This is taken from https://github.com/flaxsearch/htmlcharfilter, the Lucene HTMLStripCharFilter
 * was replace because it seems to keep closing tags together with words sometimes, especially for
 * inline markup: <html><body><span><i>test</i></span></body></html>
 */

import net.htmlparser.jericho.*;
import org.apache.lucene.analysis.charfilter.BaseCharFilter;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * An HTMLCharFilter that strips out all HTML tags and translates XML entities,
 * while preserving offsets.
 */
public class HTMLCharFilter extends BaseCharFilter {

    static {
        Config.LoggerProvider = LoggerProvider.DISABLED;
    }

    private int offsetDiff = 0;
    private int totalRead = 0;
    private int currentPos = 0;
    private StringBuilder currentText = new StringBuilder();

    private final Iterator<Segment> segments;
    private final StreamedSource source;

    public HTMLCharFilter(Reader in) throws IOException {
        super(in);
        source = new StreamedSource(in);
        segments = source.iterator();
    }

    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        int i = 0;
        for ( ; i < len ; ++i) {
            int ch = read();
            if (ch == -1) break;
            cbuf[off++] = (char)ch;
        }
        return i > 0 ? i : (len == 0 ? 0 : -1);
    }

    @Override
    public int read() throws IOException {
        int ch = nextChar();
        totalRead++;
        return ch;
    }

    private int nextChar() throws IOException {
        if (currentPos >= currentText.length()) {
            if (!readNext())
                return -1;
        }
        return currentText.charAt(currentPos++);
    }

    private boolean readNext() throws IOException {
        currentText.setLength(0);
        currentPos = 0;
        while (segments.hasNext()) {
            Segment segment = segments.next();
            if (segment instanceof CharacterReference) {
                currentText.append(((CharacterReference) segment).getChar());
                offsetDiff += segment.getEnd() - segment.getBegin() - 1;
                addOffCorrectMap(totalRead + 1, offsetDiff);
                return true;
            }
            else if (segment instanceof Tag) {
                currentText.append(" ");
                offsetDiff = segment.getBegin() - totalRead;
                addOffCorrectMap(totalRead, offsetDiff);
                return true;
            }
            else {
                currentText.append(segment);
                offsetDiff = segment.getBegin() - totalRead;
                addOffCorrectMap(totalRead, offsetDiff);
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}