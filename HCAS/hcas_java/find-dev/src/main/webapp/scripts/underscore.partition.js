/**
 * Partition a sequence into groups of size n
 * partition [1, 2, 3, 4, 5, 6] 2
 * ==> [[1, 2], [3, 4], [5, 6]]
 * 
 * @Author: Liam Goodacre
 */
_.mixin({
  partition: function (items, size) {
    return _.values(_.groupBy(items, function (item, index) {
      return Math.floor(index / size);
    }));
  }
});