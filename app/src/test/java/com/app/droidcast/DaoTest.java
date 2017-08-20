package com.app.droidcast;

import android.content.SharedPreferences;
import com.app.droidcast.dao.Dao;
import com.app.droidcast.dao.DaoException;
import com.app.droidcast.dao.DaoFactory;
import com.app.droidcast.models.Model;
import com.app.droidcast.utils.DroidCastTestApp;
import com.app.droidcast.utils.ioc.IOCProviderTest;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, application = DroidCastTestApp.class)
public class DaoTest {

  @Inject DaoFactory daoFactory;
  @Inject SharedPreferences sharedPreferences;

  @Before public void init() {
    IOCProviderTest.getInstance().inject(this);
  }

  @After public void after() {
    sharedPreferences.edit().clear().commit();
  }

  /**
   * Test that initially no object of a certain type is on the storage, so exception is thrown.
   */
  @Test public void testSharePreferencesEmptyForObject() {
    try {
      daoFactory.get(TestModelNoId.class).get();
      failBecauseExceptionWasNotThrown(DaoException.class);
    } catch (DaoException e) {
      assertThat(e.getMessage()).isEqualTo(
          "Object of type: " + TestModelNoId.class.getSimpleName() + " couldn't be found");
    }
  }

  /**
   * Test that a singleton object is stored correctly in the storage.
   *
   * @throws DaoException
   */
  @Test public void testPersistSingletonModelIntoStorage() throws DaoException {

    TestModelNoId daoTestModel = new TestModelNoId();
    daoTestModel.setAnInt(1);
    daoTestModel.setAString("My String");

    TestModelNoId.SubClass subClass = daoTestModel.new SubClass();
    subClass.setAFloat(2.0f);

    daoTestModel.setASubClass(subClass);

    Dao<TestModelNoId> daoTest = daoFactory.get(TestModelNoId.class);
    daoTest.persist(daoTestModel);

    TestModelNoId readModel = daoTest.get();

    assertThat(daoTestModel.getAnInt()).isEqualTo(readModel.getAnInt());
    assertThat(daoTestModel.getAString()).isEqualTo(readModel.getAString());
    assertThat(daoTestModel.getASubClass().getAFloat()).isEqualTo(
        readModel.getASubClass().getAFloat());
  }

  /**
   * Test a model with a specific id is stored correctly in the storage.
   *
   * @throws DaoException
   */
  @Test public void testPersistingModelIntoStorage() throws DaoException {
    TestModelWithId daoTestModel = new TestModelWithId();

    Dao<TestModelWithId> daoTest = daoFactory.get(TestModelWithId.class);
    daoTest.persist(daoTestModel);

    TestModelWithId readModel = daoTest.get("myId");
    assertThat(readModel.getId()).isEqualTo("myId");
    assertThat(daoTestModel.getAnotherValue()).isEqualTo(readModel.getAnotherValue());
  }

  /**
   * Test that multiple objects of the same type with different id are correctly store in the
   * storage.
   *
   * @throws DaoException
   */
  @Test public void testPersistingMultipleModelsIntoLocalStorage() throws DaoException {
    TestModelWithId daoTestModel = new TestModelWithId();
    daoTestModel.setId("1");

    Dao<TestModelWithId> daoTest = daoFactory.get(TestModelWithId.class);
    daoTest.persist(daoTestModel);

    daoTestModel = new TestModelWithId();
    daoTestModel.setId("2");
    daoTest.persist(daoTestModel);

    List<TestModelWithId> testModelWithIdList = daoTest.getAll();
    assertThat(testModelWithIdList.size()).isEqualTo(2);
    assertThat(testModelWithIdList.get(0).getId()).isNotEqualTo(testModelWithIdList.get(1).getId());
  }

  /**
   * Test model with no id
   */
  public class TestModelNoId implements Model {
    @Getter @Setter int anInt;
    @Getter @Setter String aString;
    @Getter @Setter SubClass aSubClass;

    @Override public String getId() {
      return id;
    }

    public class SubClass {
      @Getter @Setter float aFloat;
    }
  }

  /**
   * Test model with id
   */
  public class TestModelWithId implements Model {
    @Getter @Setter public String id = "myId";
    @Getter @Setter String anotherValue = "another string";
  }
}
