package com.example.salonflow;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Disabled("Manual DB inspection test only - disabled in CI")
@SpringBootTest
class SalonflowApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void inspectDatabase() {
		System.out.println("=== START DATABASE INSPECTION ===");
		try {
			// Query all sequences
			List<Map<String, Object>> seqs = jdbcTemplate.queryForList(
				"SELECT c.relname FROM pg_class c WHERE c.relkind = 'S'"
			);
			System.out.println("All Sequences:");
			seqs.forEach(System.out::println);

			// Query branch_hours structure
			List<Map<String, Object>> columns = jdbcTemplate.queryForList(
				"SELECT column_name, column_default, data_type FROM information_schema.columns WHERE table_name = 'branch_hours'"
			);
			System.out.println("\nbranch_hours Columns:");
			columns.forEach(System.out::println);

		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		System.out.println("=== END DATABASE INSPECTION ===");
	}

}
