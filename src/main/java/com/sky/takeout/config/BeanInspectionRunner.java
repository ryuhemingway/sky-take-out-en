package com.sky.takeout.config;

import com.sky.takeout.mapper.DishMapper;
import com.sky.takeout.service.DishService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Temporary learning helper.
 * Proves two things at startup:
 *   1) Spring creates beans in dependency order.
 *   2) getBean() always returns the same singleton instance.
 * Safe to delete when the lesson is done.
 */
@Component
public class BeanInspectionRunner implements ApplicationRunner {

    private final ApplicationContext context;

    public BeanInspectionRunner(ApplicationContext context) {
        this.context = context;
        System.out.println("=== [1] BeanInspectionRunner constructed: ApplicationContext injected ===");
    }

    @Override
    public void run(ApplicationArguments args) {
        DishService first = context.getBean(DishService.class);
        DishService second = context.getBean(DishService.class);

        System.out.println("=== [2] DishService bean class   : " + first.getClass().getName());
        System.out.println("=== [3] first  identityHashCode  : " + System.identityHashCode(first));
        System.out.println("=== [4] second identityHashCode  : " + System.identityHashCode(second));
        System.out.println("=== [5] same instance?           : " + (first == second));

        DishMapper firstMapper = context.getBean(DishMapper.class);
        DishMapper secondMapper = context.getBean(DishMapper.class);

        System.out.println("=== [6] DishMapper bean class    : " + firstMapper.getClass().getName());
        System.out.println("=== [7] DishMapper same instance?: " + (firstMapper == secondMapper));
        System.out.println("=== [8] bean definitions total   : " + context.getBeanDefinitionCount());
    }
}