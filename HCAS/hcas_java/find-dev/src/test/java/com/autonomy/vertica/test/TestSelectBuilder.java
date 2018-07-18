package com.autonomy.vertica.test;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.autonomy.vertica.common.JoinType;
import com.autonomy.vertica.common.SelectBuilder;
@Ignore
public class TestSelectBuilder {

	@Test
	public void testSelectBuilder() {
		SelectBuilder selectBuilder = new SelectBuilder("admissions_new", "mimic2v26");
		//selectBuilder.column("hadm_id", "admissions_new", "count", true);
		selectBuilder.offset("10");
		selectBuilder.limit("5");
		selectBuilder.join(JoinType.InnerJoin, "admissions_new", "demographic_detail", "hadm_id", "hadm_id", null, null);
		selectBuilder.where("marital_status_descr", "demographic_detail", "in", "'\"MARRIED\"'");
		System.out.println(selectBuilder);
	}

	@Test
	public void testSelectBuilderString() {
		fail("Not yet implemented");
	}

	@Test
	public void testSelectBuilderStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testColumnStringStringBoolean() {
		fail("Not yet implemented");
	}

	@Test
	public void testColumnStringStringStringBoolean() {
		fail("Not yet implemented");
	}

	@Test
	public void testColumnStringStringBooleanBoolean() {
		fail("Not yet implemented");
	}

	@Test
	public void testColumnStringStringStringBooleanBoolean() {
		fail("Not yet implemented");
	}

	@Test
	public void testFrom() {
		fail("Not yet implemented");
	}

	@Test
	public void testGroupBy() {
		fail("Not yet implemented");
	}

	@Test
	public void testHaving() {
		fail("Not yet implemented");
	}

	@Test
	public void testJoinJoinTypeStringStringStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testJoinString() {
		fail("Not yet implemented");
	}

	@Test
	public void testInnerJoin() {
		fail("Not yet implemented");
	}

	@Test
	public void testLeftJoin() {
		fail("Not yet implemented");
	}

	@Test
	public void testLeftOuterJoin() {
		fail("Not yet implemented");
	}

	@Test
	public void testOrderBy() {
		fail("Not yet implemented");
	}

	@Test
	public void testToString() {
		fail("Not yet implemented");
	}

	@Test
	public void testWhereString() {
		fail("Not yet implemented");
	}

	@Test
	public void testWhereStringStringStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSchema() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSchema() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsUseSchemaQualifier() {
		fail("Not yet implemented");
	}

}
