package me.kpali.wolfflow.core.querier.impl;

import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNull;

@SpringBootTest
public class DefaultTaskFlowQuerierTest extends AbstractTestNGSpringContextTests {

    @Autowired
    ITaskFlowQuerier taskFlowQuerier;

    @Test
    public void testGetTaskFlow() {
        assertNull(this.taskFlowQuerier.getTaskFlow(1L));
    }

    @Test
    public void testListCronTaskFlow() {
        assertNull(this.taskFlowQuerier.listCronTaskFlow());
    }
}