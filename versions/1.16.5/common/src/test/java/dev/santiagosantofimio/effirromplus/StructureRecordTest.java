package dev.santiagosantofimio.effirromplus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.santiagosantofimio.effirromplus.structure.StructureRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StructureRecordTest {
	private static final double SECOND = 0.15;
	private static final double THIRD = 0.03;

	private static StructureRecord structure(int chests, int chunks) {
		Set<Long> expected = new java.util.TreeSet<>();
		for (long chunk = 0; chunk < chunks; chunk++) {
			expected.add(chunk);
		}
		StructureRecord record = StructureRecord.expecting(expected);
		for (int i = 0; i < chests; i++) {
			record.register("chest-" + i);
		}
		for (long chunk = 0; chunk < chunks; chunk++) {
			record.chunkLoaded(chunk);
		}
		return record;
	}

	private static List<String> booked(StructureRecord record) {
		return record.chestIds().stream().filter(id -> record.bookOf(id) != StructureRecord.NO_BOOK).toList();
	}

	@Test
	void everyCompletedStructureHasAtLeastOneBook() {
		for (int seed = 0; seed < 5000; seed++) {
			int chests = 1 + seed % 12;
			StructureRecord record = structure(chests, 4);
			assertTrue(record.decideIfReady(new Random(seed), SECOND, THIRD));
			int books = booked(record).size();
			assertTrue(books >= 1 && books <= Math.min(3, chests), "books " + books + " in " + chests + " chests");
		}
	}

	@Test
	void extraBooksAppearWithTheConfiguredFrequency() {
		int rolls = 200000;
		int atLeastTwo = 0;
		int three = 0;
		for (int seed = 0; seed < rolls; seed++) {
			StructureRecord record = structure(10, 1);
			record.decideIfReady(new Random(seed), SECOND, THIRD);
			int books = booked(record).size();
			atLeastTwo += books >= 2 ? 1 : 0;
			three += books == 3 ? 1 : 0;
		}
		assertEquals(SECOND, (double) atLeastTwo / rolls, 0.005);
		assertEquals(THIRD, (double) three / rolls, 0.003);
	}

	@Test
	void bothBookKindsAppearEvenly() {
		int unbreaking = 0;
		int efficiency = 0;
		for (int seed = 0; seed < 20000; seed++) {
			StructureRecord record = structure(1, 1);
			record.decideIfReady(new Random(seed), 0.0, 0.0);
			if (record.bookOf("chest-0") == StructureRecord.UNBREAKING_BOOK) {
				unbreaking++;
			} else if (record.bookOf("chest-0") == StructureRecord.EFFICIENCY_BOOK) {
				efficiency++;
			}
		}
		assertEquals(20000, unbreaking + efficiency);
		assertEquals(0.5, unbreaking / 20000.0, 0.02);
	}

	@Test
	void whichChestHasTheBookDoesNotDependOnTheOrderTheyAreOpened() {
		for (int seed = 0; seed < 300; seed++) {
			StructureRecord first = structure(9, 3);
			StructureRecord second = structure(9, 3);
			first.decideIfReady(new Random(seed), SECOND, THIRD);
			second.decideIfReady(new Random(seed), SECOND, THIRD);
			List<String> order = new ArrayList<>(first.chestIds());
			Collections.shuffle(order, new Random(seed + 1));
			for (String id : order) {
				first.open(id, new Random(99), SECOND, THIRD);
			}
			assertEquals(booked(second), booked(first));
		}
	}

	@Test
	void openedChestsReturnTheirBookOnlyOnce() {
		StructureRecord record = structure(1, 1);
		record.decideIfReady(new Random(1), 0.0, 0.0);
		int book = record.bookOf("chest-0");
		assertEquals(book, record.open("chest-0", new Random(2), 0.0, 0.0));
		assertEquals(StructureRecord.NO_BOOK, record.open("chest-0", new Random(2), 0.0, 0.0));
	}

	@Test
	void openingBeforeTheStructureIsCompleteStillKeepsTheGuarantee() {
		for (int seed = 0; seed < 2000; seed++) {
			StructureRecord record = StructureRecord.expecting(Set.of(1L, 2L, 3L));
			record.register("a");
			record.chunkLoaded(1);
			assertFalse(record.isComplete());
			assertFalse(record.decideIfReady(new Random(seed), SECOND, THIRD));
			int found = record.open("a", new Random(seed), SECOND, THIRD);
			assertTrue(found != StructureRecord.NO_BOOK, "the only known chest must carry the book");
			assertTrue(record.isDecided());
		}
	}

	@Test
	void earlyDrawSkipsChestsAlreadyOpened() {
		for (int seed = 0; seed < 2000; seed++) {
			StructureRecord record = StructureRecord.expecting(Set.of(1L, 2L));
			record.register("a");
			record.register("b");
			record.chunkLoaded(1);
			int fromA = record.open("a", new Random(seed), SECOND, THIRD);
			int fromB = record.open("b", new Random(seed), SECOND, THIRD);
			assertTrue(fromA != StructureRecord.NO_BOOK || fromB != StructureRecord.NO_BOOK);
		}
	}

	@Test
	void structureWithoutChestsWaitsInsteadOfDeciding() {
		StructureRecord record = structure(0, 2);
		assertFalse(record.decideIfReady(new Random(1), SECOND, THIRD));
		assertFalse(record.isDecided());
		record.register("late");
		assertTrue(record.decideIfReady(new Random(1), SECOND, THIRD));
		assertTrue(record.bookOf("late") != StructureRecord.NO_BOOK);
	}

	@Test
	void recordSurvivesSerialization() {
		StructureRecord record = structure(6, 3);
		record.decideIfReady(new Random(4), SECOND, THIRD);
		record.open("chest-2", new Random(4), SECOND, THIRD);
		StructureRecord copy = StructureRecord.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, record)
				.flatMap(json -> StructureRecord.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json)).result().orElseThrow();
		assertEquals(booked(record), booked(copy));
		assertEquals(record.isDecided(), copy.isDecided());
		assertTrue(copy.isOpened("chest-2"));
		assertTrue(copy.isComplete());
	}
}
