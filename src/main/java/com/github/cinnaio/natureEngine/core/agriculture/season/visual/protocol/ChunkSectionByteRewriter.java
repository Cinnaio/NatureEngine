package com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * 重写 chunk section 序列化数据中的 biome paletted container（按 palette entries 替换 raw id）。
 *
 * 注意：这里不解析/修改 data long[]（索引位图），只替换 palette entries，从而避免重排。
 * 该策略对 indirect palette（bitsPerEntry <= 3）最稳健；对 direct palette（global）将不做替换。
 */
public final class ChunkSectionByteRewriter {

    public record Result(byte[] out, Stats stats) {}

    public static final class Stats {
        public int sectionsAttempted;
        public int containersParsed;
        public int biomeContainersTouched;
        public int paletteEntriesReplaced;
        public int directPaletteSkipped;
        public boolean failed;
        public String failReason;
    }

    public Result rewrite(byte[] in, int sectionCount, Map<Integer, Integer> biomeIdMap, Integer defaultToBiomeId) {
        Stats stats = new Stats();
        if (in == null || in.length == 0 || sectionCount <= 0 || ((biomeIdMap == null || biomeIdMap.isEmpty()) && defaultToBiomeId == null)) {
            return new Result(in, stats);
        }
        try {
            VarIntCursor c = new VarIntCursor(in);
            Out out = new Out(in.length);
            stats.sectionsAttempted = sectionCount;
            for (int i = 0; i < sectionCount; i++) {
                // 每个 section 序列化前都有 nonEmptyBlockCount（short）
                int nonEmpty = c.readUnsignedShort();
                out.writeShort(nonEmpty);
                // block states container（不改，只解析并原样写出）
                parseAndCopyPalettedContainer(c, out, null, null, stats);
                // biome container（替换 palette entries）
                parseAndCopyPalettedContainer(c, out, biomeIdMap, defaultToBiomeId, stats);
                stats.biomeContainersTouched++;
            }
            // 如果剩余还有字节（理论上不应有，但某些实现可能追加内容），原样复制
            while (c.hasRemaining()) {
                out.writeByte(c.readUnsignedByte());
            }
            return new Result(out.toByteArray(), stats);
        } catch (Throwable t) {
            stats.failed = true;
            stats.failReason = t.getClass().getSimpleName() + ": " + t.getMessage();
            return new Result(in, stats);
        }
    }

    /**
     * PalettedContainer 序列化（简化）：
     * - bitsPerEntry: unsigned byte
     * - palette:
     *   - if bitsPerEntry == 0: single value (varint)
     *   - else if bitsPerEntry <= 8: indirect palette: paletteLen(varint) + paletteLen * varint
     *   - else: direct/global: no palette entries
     * - dataArrayLen: varint
     * - dataArrayLen * long
     */
    private void parseAndCopyPalettedContainer(VarIntCursor c, Out out, Map<Integer, Integer> replacePaletteIds, Integer defaultToId, Stats stats) {
        int bitsPerEntry = c.readUnsignedByte();
        out.writeByte(bitsPerEntry);
        stats.containersParsed++;

        if (bitsPerEntry == 0) {
            int id = c.readVarInt();
            if (replacePaletteIds != null) {
                Integer to = replacePaletteIds.get(id);
                if (to != null) {
                    id = to;
                    stats.paletteEntriesReplaced++;
                }
            } else if (defaultToId != null && id != defaultToId) {
                id = defaultToId;
                stats.paletteEntriesReplaced++;
            }
            out.writeVarInt(id);
            int dataLen = c.readVarInt();
            out.writeVarInt(dataLen);
            c.copyLongs(out, dataLen);
            return;
        }

        if (bitsPerEntry <= 8) {
            int paletteLen = c.readVarInt();
            out.writeVarInt(paletteLen);
            for (int i = 0; i < paletteLen; i++) {
                int id = c.readVarInt();
                if (replacePaletteIds != null) {
                    Integer to = replacePaletteIds.get(id);
                    if (to != null) {
                        id = to;
                        stats.paletteEntriesReplaced++;
                    }
                } else if (defaultToId != null && id != defaultToId) {
                    id = defaultToId;
                    stats.paletteEntriesReplaced++;
                }
                out.writeVarInt(id);
            }
            int dataLen = c.readVarInt();
            out.writeVarInt(dataLen);
            c.copyLongs(out, dataLen);
            return;
        }

        // direct/global palette：无法只改 palette（没有 palette）
        stats.directPaletteSkipped++;
        int dataLen = c.readVarInt();
        out.writeVarInt(dataLen);
        c.copyLongs(out, dataLen);
    }

    /**
     * 解析/写入 VarInt 的光标。
     * 为了允许“写出后回退覆盖最后一个 varint”，这里把写出动作交给扩展的 ByteArrayOutputStream。
     */
    private static final class VarIntCursor {
        private final byte[] in;
        private int pos;

        VarIntCursor(byte[] in) {
            this.in = in;
        }

        boolean hasRemaining() {
            return pos < in.length;
        }

        int readUnsignedByte() {
            return in[pos++] & 0xFF;
        }

        int readUnsignedShort() {
            int hi = in[pos++] & 0xFF;
            int lo = in[pos++] & 0xFF;
            return (hi << 8) | lo;
        }

        int readVarInt() {
            int numRead = 0;
            int result = 0;
            byte read;
            do {
                read = in[pos++];
                int value = (read & 0b01111111);
                result |= (value << (7 * numRead));

                numRead++;
                if (numRead > 5) {
                    throw new IllegalStateException("VarInt too big");
                }
            } while ((read & 0b10000000) != 0);
            return result;
        }

        void copyLongs(Out out, int longs) {
            int bytes = longs * 8;
            out.write(in, pos, bytes);
            pos += bytes;
        }
    }

    /**
     * 写出工具（VarInt + 原样 bytes）。
     */
    private static final class Out extends ByteArrayOutputStream {
        Out(int initialSize) {
            super(initialSize);
        }

        void writeByte(int b) {
            this.write(b & 0xFF);
        }

        void writeShort(int v) {
            this.write((v >>> 8) & 0xFF);
            this.write(v & 0xFF);
        }

        void writeVarInt(int value) {
            int v = value;
            while (true) {
                if ((v & ~0x7F) == 0) {
                    this.write(v);
                    return;
                }
                this.write((v & 0x7F) | 0x80);
                v >>>= 7;
            }
        }
    }
}

