package examples.snakes;

import jbotsim.*;
import jbotsimx.misc.UtilClock;
import jbotsimx.topology.TopologyGeneratorFactory;

import java.util.*;

public class myTopology extends Topology {
    private int SNAKE_NODE_SIZE = 2 * Node.DEFAULT_SIZE;
    private int SNAKE_LINK_WIDTH = 5 * Link.DEFAULT_WIDTH;
    private Map<Integer, Snake> snake_map;
    private final int maxTry = 100;
    public boolean isInitialize = false;

    private class Snake {
        private int num;
        public int size;
        private ArrayList<Node> snakeNodes;
        public boolean isMergeReady;
        public boolean isReset = false;

        public Snake(int num, int size) {
            this.size = size;
            this.num = num;
            this.isMergeReady = false;
            snakeNodes = new ArrayList<>(size);

            // randomly choose a node
            Node cur = chooseSnakeHead();
            if (cur == null) {
                return;
            }

            // set properties of this node
            cur.flag = num;
            cur.isHead = true;
            cur.isWaiting = false;
            snakeNodes.add(cur);


            for (int i = 1; i < size; ++i) {
                Node next = chooseNext(cur, false);
                if (next == null) {
                    return;
                }

                snakeNodes.add(next);
                next.flag = num;
                next.isHead = true;
                next.isWaiting = false;
                cur = next;
            }

            cur.isLast = true;

        }

        private Node chooseSnakeHead() {
            int n = getNodes().size();
            Node headNode = null;

            int i = 0;
            while (i < maxTry) {
                Random random = new Random();
                int head = random.nextInt(n);
                headNode = getNodes().get(head);
                if (headNode.flag == -1)
                    break;
            }
            return headNode;
        }

        private Node chooseNext(Node node, boolean isHeadMove) {
            List<Node> neighbors = node.getNeighbors();
            ArrayList<Integer> candidates = new ArrayList<>();

            for (int i = 0; i < neighbors.size(); ++i) {
                Node next_node = neighbors.get(i);
                if (next_node.flag == -1) {
                    candidates.add(i);
                } else if (!isHeadMove) {
                    continue;
                } else { // wait for another snake
                    if (next_node.flag != num) {
                        Node tmp = next_node;
                        while (tmp.isWaiting) {
                            Node mergeNode = snake_map.get(tmp.flag).snakeNodes.get(0).mergingNode;
                            if (mergeNode.flag == num) { // wait for itself, break out
                                break;
                            } else {
                                tmp = mergeNode;
                            }
                        }
                        if (!tmp.isWaiting) {
                            return next_node;
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                return null;
            }
            Random random = new Random();
            int next = candidates.get(random.nextInt(candidates.size()));
            return neighbors.get(next);
        }

        private void moveHead(Node cur_head) {
            if (!cur_head.isWaiting) { // the snake is not waiting
                Node next_head = chooseNext(cur_head, true);
                if (next_head == null) {
                    // snake dead, replace
                    System.out.println("Reset snake " + num);
                    int num = this.num;
                    int len = this.snakeNodes.size();
                    int retry;
                    Node pre = null;
                    // clear snake
                    for (int i = 0; i < len; ++i) {
                        Node cur = this.snakeNodes.get(i);
                        resetNode(cur, pre);
                        pre = cur;
                    }
                    // snake_map.remove(num); something wrong with the remove during iteration

                    for (retry = 0; retry < maxTry; ++retry) {
                        Snake s = new Snake(num, size);
                        int snakeSize = s.snakeNodes.size();
                        if (snakeSize == 0) {
                            // Cannot select snake head, exit program
                            return;
                        }

                        if (snakeSize < size) {
                            // clear flag
                            for (Node n : s.snakeNodes) {
                                n.flag = -1;
                                n.isLast = false;
                            }
                            continue; // rebuild the snake
                        } else {
                            drawSnake(s);
                            snake_map.put(num, s);
                            isReset = true;
                            break;
                        }
                    }

                    if (retry == maxTry) {
                        //System.out.println("Cannot replace a snake.");
                        return;
                    }

                    return;
                }
                if (next_head.flag != -1) {
                    System.out.println("Snake " + num + " head at " + cur_head.getID() + " stops for snake "
                            + next_head.flag + " head at " + next_head.getID());
                    // wait for merging
                    cur_head.setColor(Color.RED);
                    if (next_head.isLast) {
                        // merge without waiting
                        System.out.println("Merge without waiting");
                        cur_head.mergingNode = next_head;
                        this.isMergeReady = true;
                        return;
                    } else {
                        // wait
                        for (Node n : this.snakeNodes) {
                            n.isWaiting = true;
                        }
                        cur_head.mergingNode = next_head;
                        return;
                    }
                }
                // set next head and insert it to the node list
                System.out.println("Snake " + num + " head moves from " + cur_head.getID() + " to " + next_head.getID());
                next_head.setColor(Color.GREEN);
                next_head.setSize(SNAKE_NODE_SIZE);
                next_head.flag = num;
                next_head.isHead = true;
                next_head.isWaiting = false;
                snakeNodes.add(0, next_head);

            } else {
                System.out.println("Snake " + num + " head at " + cur_head.getID() + " waiting.");
                if (cur_head.mergingNode.flag == -1) {
                    // another snake replaced, free the waiting snake
                    for (Node n : snakeNodes) {
                        n.isWaiting = false;
                    }
                    return;
                }
                if (cur_head.mergingNode.isLast) {
                    // merge
                    this.isMergeReady = true;
                }
                return;
            }
        }

        private void moveBody() {

            Node pre = snakeNodes.get(0);
            Node cur = pre;
            int index = 0;
            int last_index = snakeNodes.size() - 1;
            if (last_index == 0) return;
            // if snake is not waiting
            if (!pre.isWaiting) {
                for (index = 1; index < last_index; ++index) {
                    cur = snakeNodes.get(index);
                    if (cur.isHead != pre.isHead) break;
                    if (cur.isHead) {
                        setTail(cur, pre);
                    } else {
                        setHead(cur, pre);
                    }
                    cur.isLast = false;
                    pre = cur;
                }

            }


            if (index == last_index) {
                cur = snakeNodes.get(index);
                // if the last node is tail, delete the link and the node
                if (!cur.isHead) {
                    // delete the link
                    Link l = cur.getCommonLinkWith(pre);
                    // ORANGE links show elasticity
                    l.setColor(Link.DEFAULT_COLOR);
                    l.setWidth(Link.DEFAULT_WIDTH);
                    // delete the last node
                    cur.flag = -1;
                    cur.setColor(Node.DEFAULT_COLOR);
                    snakeNodes.remove(cur);
                    pre.isLast = true;
                } else {
                    if (pre.isHead)
                        setTail(cur, pre);
                }
                return;
            }


            while (true) {
                while (index < last_index && cur.isHead) {
                    cur.isLast = false;
                    ++index;
                    pre = cur;
                    cur = snakeNodes.get(index);
                }
                if (index == last_index) {
                    // if the last node is tail, delete the link and the node
                    cur = snakeNodes.get(index);
                    if (!cur.isHead) {
                        // delete the link
                        Link l = cur.getCommonLinkWith(pre);
                        // ORANGE links show elasticity
                        l.setColor(Link.DEFAULT_COLOR);
                        l.setWidth(Link.DEFAULT_WIDTH);
                        // delete the last node
                        cur.flag = -1;
                        cur.setColor(Node.DEFAULT_COLOR);
                        snakeNodes.remove(cur);
                        pre.isLast = true;
                    }
                    return;
                }
                setHead(cur, pre);
                pre = cur;
                ++index;

                for ( ; index < last_index; ++index) {
                    cur = snakeNodes.get(index);
                    cur.isLast = false;
                    if (cur.isHead != pre.isHead) break;
                    if (cur.isHead) {
                        setTail(cur, pre);
                    } else {
                        setHead(cur, pre);
                    }
                    pre = cur;
                }

                if (index == last_index) {
                    // if the last node is tail, delete the link and the node
                    cur = snakeNodes.get(index);
                    if (!cur.isHead) {
                        // delete the link
                        Link l = cur.getCommonLinkWith(pre);
                        // ORANGE links show elasticity
                        l.setColor(Link.DEFAULT_COLOR);
                        l.setWidth(Link.DEFAULT_WIDTH);
                        // delete the last node
                        cur.flag = -1;
                        cur.setColor(Node.DEFAULT_COLOR);
                        snakeNodes.remove(cur);
                        pre.isLast = true;
                    }
                    break;
                }
            }
        }

        private void setHead(Node cur, Node pre) {
            System.out.println("set cur ID " + cur.getID() + " head and delete a link with pre ID " + pre.getID());
            cur.isHead = true;
            cur.setColor(Color.RED);
            cur.setSize(SNAKE_NODE_SIZE);
            // delete the link
            Link l = cur.getCommonLinkWith(pre);
            // ORANGE links show elasticity
            l.setColor(Link.DEFAULT_COLOR);
            l.setWidth(Link.DEFAULT_WIDTH);
        }

        private void setTail(Node cur, Node pre) {
            System.out.println("set cur ID " + cur.getID() + " tail and draw a link with pre ID " + pre.getID());
            cur.isHead = false;
            cur.setColor(Color.ORANGE);
            cur.setSize(Node.DEFAULT_SIZE);
            pre.isLast = false;
            // draw the link
            Link l = cur.getCommonLinkWith(pre);
            // ORANGE links show elasticity
            l.setColor(Color.ORANGE);
            l.setWidth(SNAKE_LINK_WIDTH);
        }

        private void resetNode(Node cur, Node pre) {
            cur.isWaiting = false;
            cur.isLast = false;
            cur.mergingNode = null;
            cur.flag = -1;
            cur.setColor(Node.DEFAULT_COLOR);
            cur.setSize(Node.DEFAULT_SIZE);
            if (!cur.isHead) {
                // delete the link
                Link l = cur.getCommonLinkWith(pre);
                // ORANGE links show elasticity
                l.setColor(Link.DEFAULT_COLOR);
                l.setWidth(Link.DEFAULT_WIDTH);
            }
        }
    }

    private class TriangleGridGenerator {
        protected int xOrder;
        protected int yOrder;

        TriangleGridGenerator(int xOrder, int yOrder) {
            this.xOrder = xOrder;
            this.yOrder = yOrder;
        }

        public void generate(Topology tp, TopologyGeneratorFactory gf) {
            try {
                generateTriangleGrid(tp, gf);
            } catch (ReflectiveOperationException e) {
                System.err.println(e.getMessage());
            }
        }

        protected Node[][] generateTriangleGrid(Topology tp, TopologyGeneratorFactory gf) throws ReflectiveOperationException {
            Node[][] nodes = generateNodes(tp, gf);
            if (gf.isWired()) {
                Link.Type type = gf.isDirected() ? Link.Type.DIRECTED : Link.Type.UNDIRECTED;
                for (int i = 0; i < yOrder; i++) {
                    boolean isOddRow = (i & 1) == 1 ? true : false;
                    for (int j = 0; j < xOrder; j++) {
                        boolean isLeftest = j == 0 ? true : false;
                        boolean isRightest = j == xOrder - 1 ? true : false;
                        Node n = nodes[i][j];
                        if (j < xOrder - 1) {
                            Link l = new Link(n, nodes[i][j + 1], type); // link the right node in the same row
                            tp.addLink(l);
                        }

                        if (i < yOrder - 1) {
                            if (isOddRow) {
                                Link l1 = new Link(n, nodes[i + 1][j], type);
                                tp.addLink(l1);
                                if (!isRightest) {
                                    Link l2 = new Link(n, nodes[i + 1][j + 1], type);
                                    tp.addLink(l2);
                                }
                            } else {
                                Link l1 = new Link(n, nodes[i + 1][j], type);
                                tp.addLink(l1);
                                if (!isLeftest) {
                                    Link l2 = new Link(n, nodes[i + 1][j - 1], type);
                                    tp.addLink(l2);
                                }
                            }
                        }
                    }
                }
            }
            return nodes;
        }

        private Node[][] generateNodes(Topology tp, TopologyGeneratorFactory gf) throws ReflectiveOperationException {
            Node[][] result = new Node[yOrder][];
            double x0 = gf.getAbsoluteX(tp);
            double y0 = gf.getAbsoluteY(tp);
            double xStep = gf.getAbsoluteWidth(tp) / (xOrder > 1 ? xOrder - 1 : xOrder);
            double yStep = xStep / 2 * Math.sqrt(3);

            for (int i = 0; i < yOrder; i++) {
                boolean isOddRow = (i & 1) == 1 ? true : false;
                result[i] = new Node[xOrder];
                for (int j = 0; j < xOrder; j++) {
                    Node n = gf.getNodeClass().getConstructor().newInstance();
                    if (isOddRow) {
                        n.setLocation(x0 + j * xStep + xStep / 2, y0 + i * yStep);
                    } else {
                        n.setLocation(x0 + j * xStep, y0 + i * yStep);
                    }
                    n.setCommunicationRange(0);
                    n.setColor(Node.DEFAULT_COLOR);
                    tp.addNode(n);
                    result[i][j] = n;
                }
            }

            return result;
        }
    }

    public void generateTriangleGrid(int orderX, int orderY) {
        TopologyGeneratorFactory gf = new TopologyGeneratorFactory();
        gf.setAbsoluteCoords(true);
        gf.setX(50);
        gf.setY(50);
        gf.setWidth(getWidth() - 50);
        gf.setHeight(getWidth() - 50);
        gf.setWired(true);
        gf.setNodeClass(getNodeModel("default"));
        new TriangleGridGenerator(orderX, orderY).generate(this, gf);
    }

    public ClockManager getClockManager() {
        return clockManager;
    }

    public myTopology(int len, int num) {
        super(1280, 720);
        generateTriangleGrid(30, 20);
        snake_map = new HashMap<>(num);
        int retry = 0;
        for (int i = 0; i < num && retry < maxTry; ++i, ++retry) {
            Snake s = new Snake(i, len);
            int snakeSize = s.snakeNodes.size();
            if (snakeSize == 0) {
                // Cannot select snake head, exit program
                return;
            }

            if (snakeSize < len) {
                // clear flag
                for (Node n : s.snakeNodes) {
                    n.flag = -1;
                }
                --i; // rebuild the snake
            } else {
                System.out.println("Snake " + i + " len " + snakeSize);
                drawSnake(s);
                snake_map.put(i, s);
            }
        }

        if (retry == maxTry) {
            return;
        }

        setClockModel(new UtilClock(getClockManager()).getClass());
        setClockSpeed(300);
        start();
        isInitialize = true;
    }

    public void drawSnake(Snake s) {
        s.snakeNodes.get(0).setSize(SNAKE_NODE_SIZE);
        s.snakeNodes.get(0).setColor(Color.GREEN);

        for (int i = 1; i < s.snakeNodes.size(); ++i) {
            s.snakeNodes.get(i).setSize(SNAKE_NODE_SIZE);
            s.snakeNodes.get(i).setColor(Color.RED);
        }
    }

    @Override
    public void onClock() {

        for (Iterator<Map.Entry<Integer, Snake>> it = snake_map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Snake> entry = it.next();
            Snake s = entry.getValue();

            System.out.println("Snake " + s.num + " key " + entry.getKey() + " len " + s.snakeNodes.size());
            Node head = s.snakeNodes.get(0);
            s.moveHead(head);
            if (s.isReset) continue;
            s.moveBody();
            if (s.isMergeReady) {
                // merge the snake to another one
                merge(snake_map.get(head.mergingNode.flag), s);
                it.remove();
            }
        }
    }

    public void merge(Snake s1, Snake s2) {
        Node s1_tail = s1.snakeNodes.get(s1.snakeNodes.size() - 1);
        Node s2_head = s2.snakeNodes.get(0);
        s1_tail.isLast = false;

        s2_head.setColor(Color.RED);

        for (Node n : s2.snakeNodes) {
            n.flag = s1.num;
            n.isWaiting = s1_tail.isWaiting;
        }

        s1.snakeNodes.addAll(s2.snakeNodes);
        s1.size += s2.size;
        System.out.println("Snake " + s1.num + " merged to snake " + s2.num);
    }
}
