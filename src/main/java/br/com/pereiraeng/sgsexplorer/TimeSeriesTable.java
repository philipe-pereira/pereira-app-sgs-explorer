package br.com.pereiraeng.sgsexplorer;

import java.util.BitSet;

public final class TimeSeriesTable {
    private int size;

    private int[] t;            // chaves (epochDay), ordenadas
    private double[] valor;     // coluna principal
    private BitSet bloqueado;   // flag booleana (1 bit por ponto)

    public TimeSeriesTable(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        this.t = new int[Math.max(8, initialCapacity)];
        this.valor = new double[Math.max(8, initialCapacity)];
        this.bloqueado = new BitSet(Math.max(8, initialCapacity));
        this.size = 0;
    }

    public TimeSeriesTable() {
        this(128);
    }

    public int size() {
        return size;
    }

    public int timeAt(int index) {
        checkIndex(index);
        return t[index];
    }

    public double valorAt(int index) {
        checkIndex(index);
        return valor[index];
    }

    public boolean bloqueadoAt(int index) {
        checkIndex(index);
        return bloqueado.get(index);
    }

    /** Retorna o índice da chave t (epochDay) ou -1 se não existe. */
    public int indexOfTime(int time) {
        int idx = binarySearch(time);
        return (idx >= 0) ? idx : -1;
    }

    /**
     * Insere/atualiza um ponto.
     * - Se time já existe: atualiza colunas (overwrite).
     * - Se não existe: insere mantendo ordem (shift em arrays + BitSet).
     *
     * Custo:
     * - update: O(log n)
     * - insert: O(n) por causa do shift (ótimo se inserções no meio são raras).
     */
    public void put(int time, double valorValue, boolean bloqueadoValue) {
        int idx = binarySearch(time);
        if (idx >= 0) {
            // update
            valor[idx] = valorValue;
            bloqueado.set(idx, bloqueadoValue);
            return;
        }

        // insert
        int insertAt = -idx - 1;
        ensureCapacity(size + 1);

        if (insertAt < size) {
            // shift arrays
            System.arraycopy(t, insertAt, t, insertAt + 1, size - insertAt);
            System.arraycopy(valor, insertAt, valor, insertAt + 1, size - insertAt);
            // shift BitSet
            shiftBitSetRight(insertAt, 1);
        }

        t[insertAt] = time;
        valor[insertAt] = valorValue;
        bloqueado.set(insertAt, bloqueadoValue);
        size++;
    }

    /** Busca por intervalo [fromTime, toTime] (inclusive), retornando faixa de índices [fromIdx, toIdxExclusive). */
    public IntRange rangeInclusive(int fromTime, int toTime) {
        if (size == 0) return new IntRange(0, 0);
        if (fromTime > toTime) return new IntRange(0, 0);

        int fromIdx = lowerBound(fromTime);
        int toIdxExcl = upperBound(toTime); // exclusivo

        if (fromIdx < 0) fromIdx = 0;
        if (toIdxExcl < fromIdx) toIdxExcl = fromIdx;
        return new IntRange(fromIdx, Math.min(toIdxExcl, size));
    }

    /** Retorna uma cópia compacta (trim to size) — útil depois de construir. */
    public void compact() {
        if (t.length == size) return;

        int[] nt = new int[size];
        double[] nv = new double[size];
        System.arraycopy(t, 0, nt, 0, size);
        System.arraycopy(valor, 0, nv, 0, size);

        BitSet nb = new BitSet(size);
        for (int i = bloqueado.nextSetBit(0); i >= 0 && i < size; i = bloqueado.nextSetBit(i + 1)) {
            nb.set(i);
        }

        this.t = nt;
        this.valor = nv;
        this.bloqueado = nb;
    }

    // ---------- internals ----------

    private void ensureCapacity(int minCapacity) {
        int cap = t.length;
        if (cap >= minCapacity) return;

        int newCap = cap + (cap >> 1) + 1; // ~1.5x
        if (newCap < minCapacity) newCap = minCapacity;

        int[] nt = new int[newCap];
        double[] nv = new double[newCap];

        System.arraycopy(t, 0, nt, 0, size);
        System.arraycopy(valor, 0, nv, 0, size);

        // BitSet cresce automaticamente; só mantemos a instância.
        this.t = nt;
        this.valor = nv;
    }

    /**
     * binarySearch padrão em array ordenado:
     * - retorna idx >= 0 se achou
     * - retorna -(insertionPoint) - 1 se não achou
     */
    private int binarySearch(int key) {
        int lo = 0;
        int hi = size - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int v = t[mid];

            if (v < key) {
                lo = mid + 1;
            } else if (v > key) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    /** Primeiro índice i tal que t[i] >= key. */
    private int lowerBound(int key) {
        int lo = 0, hi = size;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (t[mid] < key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Primeiro índice i tal que t[i] > key. */
    private int upperBound(int key) {
        int lo = 0, hi = size;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (t[mid] <= key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Shift à direita no BitSet a partir de 'fromIndex' por 'count' posições (count>0). */
    private void shiftBitSetRight(int fromIndex, int count) {
        if (count <= 0) return;
        if (fromIndex < 0 || fromIndex > size) throw new IndexOutOfBoundsException();

        // Copia bits de trás pra frente pra não sobrescrever
        for (int i = size - 1; i >= fromIndex; i--) {
            boolean bit = bloqueado.get(i);
            bloqueado.set(i + count, bit);
        }
        // Limpa o “buraco” criado
        bloqueado.clear(fromIndex, Math.min(fromIndex + count, size + count));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }

    /** Faixa de índices [from, toExclusive). */
    public static final class IntRange {
        public final int from;
        public final int toExclusive;
        public IntRange(int from, int toExclusive) {
            this.from = from;
            this.toExclusive = toExclusive;
        }
        public int length() { return Math.max(0, toExclusive - from); }
        @Override public String toString() { return "IntRange[" + from + "," + toExclusive + ")"; }
    }
}
