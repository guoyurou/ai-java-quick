package com.quick.text.SimHash;


import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.math.BigInteger;
import java.util.*;

/**
 * Created with IDEA
 * User: vector
 * Data: 2017/8/16
 * Time: 10:25
 * Description: 简单的判断两个字符串的相似度
 */
public class SimHash {

    private JiebaSegmenter segmenter = new JiebaSegmenter(); // 分词
    private List<Map<String, List<Long>>> storage = new ArrayList<Map<String, List<Long>>>();// 按照分段存储simhash，查找更快速
    private int fracCount = 4; // 默认按照4段进行simhash存储
    private int hammingThresh = 3;// 汉明距离的衡量标准

    public SimHash() {
        for (int i = 0; i < fracCount; i++)
            storage.add(new HashMap<String, List<Long>>());
    }

    public SimHash(int fracCount, int hammingThresh) {
        this.fracCount = fracCount;
        this.hammingThresh = hammingThresh;
        for (int i = 0; i < fracCount; i++)
            storage.add(new HashMap<String, List<Long>>());
    }

    /**
     * 指定文本计算simhash值
     * @param content
     * @return Long
     */
    public Long calSimhash(String content) {
        String filterContent = content.trim().replaceAll("\\p{Punct}|\\p{Space}", "");
        // 切词
        List<SegToken> lsegStr = segmenter.process(filterContent, JiebaSegmenter.SegMode.SEARCH);
        // 按照词语的hash值，计算simHashWeight(低位对齐)
        Integer[] weight = new Integer[64];
        Arrays.fill(weight, 0);
        for (SegToken st : lsegStr) {
            long wordHash = Murmur3.hash64(st.word.getBytes());
            for (int i = 0; i < 64; i++) {
                if (((wordHash >> i) & 1) == 1) weight[i] += 1;
                else weight[i] -= 1;
            }
        }

        // 计算得到Simhash值
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (weight[i] > 0) sb.append(1);
            else sb.append(0);
        }

        return new BigInteger(sb.toString(), 2).longValue();
    }

    /**
     * 判断文本是否重复
     *
     * @param content
     * @return
     */
    public boolean isDuplicate(String content) {
        Long simhash = calSimhash(content);
        List<String> lFrac = splitSimhash(simhash, fracCount);
        int dis = 0;
        for (int i = 0; i < fracCount; i++) {
            String frac = lFrac.get(i);
            Map<String, List<Long>> fracMap = storage.get(i);
            if (fracMap.containsKey(frac)) {
                for (Long simhash2 : fracMap.get(frac)) {
                    if (hamming(simhash, simhash2) < hammingThresh) return true;
                }
            }
        }
        return false;
    }

    /**
     * 按照(frac, <simhash, content>)索引进行存储
     *
     * @param simhash
     * @param content
     */
    public void store(Long simhash, String content) {
        List<String> lFrac = splitSimhash(simhash, fracCount);
        for (int i = 0; i < fracCount; i++) {
            String frac = lFrac.get(i);
            Map<String, List<Long>> fracMap = storage.get(i);
            if (fracMap.containsKey(frac)) fracMap.get(frac).add(simhash);
            else {
                List<Long> ls = new ArrayList<Long>();
                ls.add(simhash);
                fracMap.put(frac, ls);
            }
        }

    }

    // 计算汉明距离
    public int hamming(Long s1, Long s2) {
        int dis = 0;
        for (int i = 0; i < 64; i++) {
            if ((s1 >> i & 1) != (s2 >> i & 1)) dis++;
        }
        return dis;
    }

    private int hamming(String s1, String s2) {
        if (s1.length() != s2.length()) return 0;
        int dis = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) dis++;
        }
        return dis;
    }

    // 将simhash分成n段
    private List<String> splitSimhash(Long simhash, int n) {
        List<String> ls = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            sb.append(simhash >> i & 1);
            if ((i + 1) % n == 0) {
                ls.add(sb.toString());
                sb.setLength(0);
            }
        }
        return ls;
    }
}