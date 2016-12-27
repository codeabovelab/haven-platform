/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.utils;

import java.util.*;

/**
 */
public final class VersionComparator implements Comparator<String> {

    private static final String NO_SUFFIX = "";

    public static final class Builder {

        private final List<String> latest = new ArrayList<>();
        private char suffixDelimiter = '_';
        private boolean emptySuffixLast  = true;
        private final List<String> suffix = new ArrayList<>();

        /**
         * Char which prepend suffix. <p/> Default '_'.
         * @return
         */
        public char getSuffixDelimiter() {
            return suffixDelimiter;
        }

        /**
         * Char which prepend suffix. <p/> Default '_'.
         * @param suffixDelimiter
         * @return
         */
        public Builder suffixDelimiter(char suffixDelimiter) {
            setSuffixDelimiter(suffixDelimiter);
            return this;
        }

        /**
         * Char which prepend suffix. <p/> Default '_'.
         * @param suffixDelimiter
         */
        public void setSuffixDelimiter(char suffixDelimiter) {
            this.suffixDelimiter = suffixDelimiter;
        }

        /**
         * Flag which set order of empty or absent suffix. Default true.
         * @return
         */
        public boolean isEmptySuffixLast() {
            return emptySuffixLast;
        }

        /**
         * Flag which set order of empty or absent suffix. Default true.
         * @param emptySuffixLast
         * @return
         */
        public Builder emptySuffixLast(boolean emptySuffixLast) {
            setEmptySuffixLast(emptySuffixLast);
            return this;
        }

        /**
         * Flag which set order of empty or absent suffix. Default true.
         * @param emptySuffixLast
         */
        public void setEmptySuffixLast(boolean emptySuffixLast) {
            this.emptySuffixLast = emptySuffixLast;
        }

        /**
         * Add string constants which is interpreted at most latest version in adding order.
         * <p/> Like "latest" or "nightly"
         * @param item
         * @return
         */
        public Builder addLatest(String item) {
            this.latest.add(item);
            return this;
        }

        /**
         * String constants which is interpreted at most latest version in adding order.
         * <p/> Like "latest" or "nightly"
         * @return
         */
        public Collection<String> getLatest() {
            return latest;
        }


        /**
         * String constants which is interpreted at most latest version in adding order.
         * <p/> Like "latest" or "nightly"
         * @param latest
         */
        public void setLatest(Collection<String> latest) {
            this.latest.clear();
            if(latest != null) {
                this.latest.addAll(latest);
            }
        }

        /**
         * Add version suffix which is compared in adding order.
         * <p/> Suffix mut be prepend delimiter, which is defined in {@link #setSuffixDelimiter(char) }
         * <p/> Like "rc" or "GA".
         * @param suffix
         * @return
         */
        public Builder addSuffix(String suffix) {
            this.suffix.add(suffix);
            return this;
        }

        /**
         * Version suffix which is compared in adding order.
         * <p/> Suffix mut be prepend delimiter, which is defined in {@link #setSuffixDelimiter(char) }
         * <p/> Like "rc" or "GA"
         * @return
         */
        public Collection<String> getSuffix() {
            return suffix;
        }

        /**
         * Version suffix which is compared in adding order.
         * <p/> Suffix mut be prepend delimiter, which is defined in {@link #setSuffixDelimiter(char) }
         * <p/> Like "rc" or "GA"
         * @param suffix
         */
        public void setSuffix(Collection<String> suffix) {
            this.suffix.clear();
            if(suffix != null) {
                this.suffix.addAll(suffix);
            }
        }

        public VersionComparator build() {
            return new VersionComparator(this);
        }
    }

    public static final VersionComparator INSTANCE = builder().build();

    private final char suffixDelimiter;
    private final boolean emptySuffixLast;
    private final Map<String, Integer> latestMap = new TreeMap<>();
    private final Map<String, Integer> suffixMap = new TreeMap<>();

    private VersionComparator(Builder b) {
        this.suffixDelimiter = b.suffixDelimiter;
        load(b.latest, this.latestMap);
        load(b.suffix, this.suffixMap);
        this.emptySuffixLast = b.emptySuffixLast;
        suffixMap.put(NO_SUFFIX, (b.emptySuffixLast)? Integer.MAX_VALUE : Integer.MIN_VALUE);
    }

    private void load(Collection<String> src, Map<String, Integer> map) {
        int i = 0;
        for(String item: src) {
            map.put(item, i);
            i++;
        }
    }

    @Override
    public int compare(String left, String right) {
        if(left == null || right == null) {
            if(left == null) {
                return (right == null)? 0 : -1;
            }
            return 1;
        }
        if(left.equals(right)) {
            return 0;
        }
        final int llen = left.length();
        final int rlen = right.length();
        if (llen == 0 || rlen == 0) {
            return Integer.compare(llen, rlen);
        }
        Integer lo = latestMap.get(left);
        Integer ro = latestMap.get(right);
        if(lo != null || ro != null) {
            return compareOrders(lo, ro);
        }
        int lpp = 0, rpp = 0, llp = 0, rlp = 0;
        while(true) {
            llp = left.indexOf('.', llp);
            if(llp < 0) {
                llp = llen;
            }
            rlp = right.indexOf('.', rlp);
            if(rlp < 0) {
                rlp = rlen;
            }
            // sometime tokens may contains some differs than number, then we must try to compare only numbers
            // before dot, and other numbers on same level
            // calc num ends
            int lne = getNumEnd(left, lpp);
            int rne = getNumEnd(right, rpp);
            // token not always full with number, so we compare in in two steps - numbers,
            if (lne > 0 && rne > 0) {
                int res = compareTokens(left.substring(lpp, lne), right.substring(rpp, rne));
                if(res != 0) {
                    return res;
                }
                lpp = lne;
                rpp = rne;
            }
            // and then strings
            boolean end = llp == llen && rlp == rlen;
            if(lpp < llp || rpp < rlp || end) {
                String ltoken = left.substring(lpp, llp);
                String rtoken = right.substring(rpp, rlp);
                int res = compareEnds(ltoken, rtoken);
                if (res != 0 || end) {
                    return res;
                }
            }
            lpp = Math.min(++llp, llen);
            rpp = Math.min(++rlp, rlen);
        }
    }

    private int compareOrders(Integer lo, Integer ro) {
        if(lo == null) {
            return -1;
        }
        if(ro == null) {
            return 1;
        }
        return Integer.compare(lo, ro);
    }

    private int compareEnds(final String ltoken, final String rtoken) {
        final int lsp = getNumEnd(ltoken);
        final int rsp = getNumEnd(rtoken);
        String lp = (lsp < 0)? ltoken : ltoken.substring(0, lsp);
        String rp = (rsp < 0)? rtoken : rtoken.substring(0, rsp);
        int res = compareTokens(lp, rp);
        if(res == 0 && (lsp >= 0 || rsp >= 0)) {
            String ls = getSuffix(ltoken, lsp);
            String rs = getSuffix(rtoken, rsp);
            Integer lo = suffixMap.get(ls);
            Integer ro = suffixMap.get(rs);
            if(lo == null && ro == null) {
                return compareStrings(ls, rs);
            }
            if(lo == null || ro == null) {
                if(ls.isEmpty()) {
                    return emptySuffixLast? 1 : -1;
                }
                if(rs.isEmpty()) {
                    return emptySuffixLast? -1 : 1;
                }
            }
            return compareOrders(lo, ro);
        }
        return res;
    }

    private String getSuffix(String token, int pos) {
        if(pos < 0 || pos >= token.length()) {
            return NO_SUFFIX;
        }
        if(token.charAt(pos) == suffixDelimiter) {
            // when delimited exists then we need to remove it, but it no always
            return token.substring(pos + 1);
        }
        return token.substring(pos);
    }

    /**
     * Find end of number version part
     * @param token
     * @return
     */
    private int getNumEnd(String token) {
        return getNumEnd(token, 0);
    }

    private int getNumEnd(String token, int from) {
        int len = token.length();
        for(int i = from; i < len; ++i) {
            char c = token.charAt(i);
            if(c < '0' || c > '9') {
                return i;
            }
        }
        if(len < from) {
            return -1;
        }
        return len;
    }

    private int compareTokens(String ltoken, String rtoken) {
        if(ltoken.equals(rtoken)) {
            return 0;
        }
        try {
            int lti = Integer.parseInt(ltoken);
            int rti = Integer.parseInt(rtoken);
            return Integer.compare(lti, rti);
        } catch (NumberFormatException e) {
            return compareStrings(ltoken, rtoken);
        }
    }

    private int compareStrings(String left, String right) {
        int res = left.compareTo(right);
        // res can be any number, but we need only -1 0 1
        return res < 0? -1 : (res == 0? 0 : 1);
    }

    public static Builder builder() {
        return new Builder();
    }
}
