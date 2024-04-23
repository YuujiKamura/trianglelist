package com.jpaver.trianglelist

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    CsvloaderTest::class,
    SfcWriterTest::class,
    TriangleTest::class,
    TriangleListTest::class,
    PointNumberTest::class
)
class ExampleTestSuite {
    // このクラスは空でOK、アノテーションが全ての設定を行います
}
