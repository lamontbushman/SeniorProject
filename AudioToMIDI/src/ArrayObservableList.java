import java.util.ArrayList;
import java.util.List;

import javafx.collections.ModifiableObservableListBase;

   public class ArrayObservableList<E> extends ModifiableObservableListBase<E> {

   private final List<E> delegate;
   
   public ArrayObservableList() {
	   delegate = new ArrayList<E>();
   }
   
   public ArrayObservableList(List<E> list) {
	   delegate = list;
   }

   public E get(int index) {
       return delegate.get(index);
   }

   public int size() {
       return delegate.size();
   }

   protected void doAdd(int index, E element) {
       delegate.add(index, element);
   }

   protected E doSet(int index, E element) {
       return delegate.set(index, element);
   }

   protected E doRemove(int index) {
       return delegate.remove(index);
   }
}