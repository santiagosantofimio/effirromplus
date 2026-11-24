package dev.santiagosantofimio.effirromplus.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.random.RandomGenerator;

public final class StructureRecord {
	public static final int NO_BOOK = 0;
	public static final int UNBREAKING_BOOK = 1;
	public static final int EFFICIENCY_BOOK = 2;

	public static final Codec<StructureRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.listOf().fieldOf("expected").forGetter(record -> new ArrayList<>(record.expectedChunks)),
			Codec.LONG.listOf().fieldOf("done").forGetter(record -> new ArrayList<>(record.loadedChunks)),
			Chest.CODEC.listOf().fieldOf("chests").forGetter(record -> new ArrayList<>(record.chests.values())),
			Codec.BOOL.fieldOf("decided").forGetter(record -> record.decided)
	).apply(instance, StructureRecord::new));

	private final Set<Long> expectedChunks = new TreeSet<>();
	private final Set<Long> loadedChunks = new TreeSet<>();
	private final Map<String, Chest> chests = new TreeMap<>();
	private boolean decided;

	public StructureRecord(List<Long> expectedChunks, List<Long> loadedChunks, List<Chest> chests, boolean decided) {
		this.expectedChunks.addAll(expectedChunks);
		this.loadedChunks.addAll(loadedChunks);
		chests.forEach(chest -> this.chests.put(chest.id, chest));
		this.decided = decided;
	}

	public static StructureRecord expecting(Set<Long> expectedChunks) {
		return new StructureRecord(new ArrayList<>(expectedChunks), List.of(), List.of(), false);
	}

	public boolean isDecided() {
		return decided;
	}

	public boolean isComplete() {
		return loadedChunks.containsAll(expectedChunks);
	}

	public Set<Long> expectedChunks() {
		return expectedChunks;
	}

	public Set<String> chestIds() {
		return chests.keySet();
	}

	public int bookOf(String chestId) {
		Chest chest = chests.get(chestId);
		return chest == null ? NO_BOOK : chest.book;
	}

	public boolean isOpened(String chestId) {
		Chest chest = chests.get(chestId);
		return chest != null && chest.opened;
	}

	public boolean register(String chestId) {
		if (chests.containsKey(chestId)) {
			return false;
		}
		chests.put(chestId, new Chest(chestId, false, NO_BOOK));
		return true;
	}

	public boolean chunkLoaded(long chunk) {
		return expectedChunks.contains(chunk) && loadedChunks.add(chunk);
	}

	public boolean decideIfReady(RandomGenerator random, double secondBookChance, double thirdBookChance) {
		if (decided || !isComplete() || chests.isEmpty()) {
			return false;
		}
		decide(random, secondBookChance, thirdBookChance);
		return true;
	}

	public int open(String chestId, RandomGenerator random, double secondBookChance, double thirdBookChance) {
		Chest chest = chests.get(chestId);
		if (chest == null || chest.opened) {
			return NO_BOOK;
		}
		if (!decided) {
			decide(random, secondBookChance, thirdBookChance);
		}
		chest.opened = true;
		return chest.book;
	}

	private void decide(RandomGenerator random, double secondBookChance, double thirdBookChance) {
		decided = true;
		List<Chest> candidates = new ArrayList<>();
		for (Chest chest : chests.values()) {
			if (!chest.opened) {
				candidates.add(chest);
			}
		}
		if (candidates.isEmpty()) {
			return;
		}
		double roll = random.nextDouble();
		int books = roll < Math.min(thirdBookChance, secondBookChance) ? 3 : roll < secondBookChance ? 2 : 1;
		for (int i = candidates.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			Chest swapped = candidates.get(i);
			candidates.set(i, candidates.get(j));
			candidates.set(j, swapped);
		}
		for (int i = 0; i < Math.min(books, candidates.size()); i++) {
			candidates.get(i).book = random.nextBoolean() ? UNBREAKING_BOOK : EFFICIENCY_BOOK;
		}
	}

	public static final class Chest {
		public static final Codec<Chest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(chest -> chest.id),
				Codec.BOOL.fieldOf("opened").forGetter(chest -> chest.opened),
				Codec.INT.fieldOf("book").forGetter(chest -> chest.book)
		).apply(instance, Chest::new));

		private final String id;
		private boolean opened;
		private int book;

		public Chest(String id, boolean opened, int book) {
			this.id = id;
			this.opened = opened;
			this.book = book;
		}
	}
}
