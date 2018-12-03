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
        private int cur_node_index;
        private boolean isMoveComplete;
        private ArrayList<Node> snakeNodes;
        private ArrayList<Link> snakeLinks;
        public boolean isMerged;

        public Snake(int num, int len) {
            this.num = num;
            this.isMerged = false;
            cur_node_index = 0;
            isMoveComplete = true;
            snakeNodes = new ArrayList<>(len);
            snakeLinks = new ArrayList<>(len); // including one elastic link

            Node cur = chooseSnakeHead();
            if (cur == null) {
                return;
            }
            cur.setSize(SNAKE_NODE_SIZE);
            cur.setColor(Color.GREEN);
            cur.flag = num;
            snakeNodes.add(cur);
            for (int i = 1; i < len; ++i) {
                Node next = chooseNext(cur, false);
                if (next == null) {
                    return;
                }
                snakeNodes.add(next);
                Link l = cur.getCommonLinkWith(next);
                l.setColor(Color.RED);
                l.setWidth(SNAKE_LINK_WIDTH);
                snakeLinks.add(l);
                next.setColor(Color.RED);
                next.setSize(SNAKE_NODE_SIZE);
                next.flag = num;
                cur = next;
            }
            cur.isTail = true;
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
                } else {
                    if (next_node.flag != num) {
                        Node tmp = next_node;
                        while (tmp.isWaiting) {
                            Node mergeNode = snake_map.get(tmp.flag).snakeNodes.get(0).mergingNode;
                            if (mergeNode.flag == num) {
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
            if (!cur_head.isWaiting) {
                Node next_head = chooseNext(cur_head, true);
                if (next_head == null) {
                    // snake dead, replace
                    int num = this.num;
                    int len = this.snakeNodes.size();
                    int retry;
                    // clear snake
                    for (Node n : snakeNodes) {
                        resetNode(n);
                    }

                    for (Link l : snakeLinks) {
                        resetLink(l);
                    }

                    for (retry = 0; retry < maxTry; ++retry) {
                        Snake s = new Snake(num, len);
                        int snakeSize = s.snakeNodes.size();
                        if (snakeSize == 0) {
                            // Cannot select snake head, exit program
                            return;
                        }

                        if (snakeSize < len) {
                            continue; // rebuild the snake
                        } else {
                            snake_map.put(num, s);
                            break;
                        }
                    }

                    if (retry == maxTry) {
                        System.out.println("Cannot replace a snake.");
                        return;
                    }

                    return;
                }
                if (next_head.flag != -1) {
                    System.out.println("Head stop.");
                    // wait for merging
                    if (next_head.isTail) {
                        // merge without waiting
                        merge(snake_map.get(next_head.flag), this);
                        return;
                    } else {
                        // wait
                        for (Node n : this.snakeNodes) {
                            n.isWaiting = true;
                        }
                        cur_head.mergingNode = next_head;
                        this.isMoveComplete = true;
                        return;
                    }
                }
                next_head.setColor(Color.GREEN);
                next_head.setSize(SNAKE_NODE_SIZE);
                next_head.flag = num;
                cur_head.setColor(Color.ORANGE);
                cur_head.setSize(Node.DEFAULT_SIZE);
                Link l = cur_head.getCommonLinkWith(next_head);
                // ORANGE links show elasticity
                l.setColor(Color.ORANGE);
                l.setWidth(SNAKE_LINK_WIDTH);
                snakeLinks.get(0).setColor(Color.ORANGE); // set the current first link to ORANGE
                snakeLinks.get(0).setWidth(SNAKE_LINK_WIDTH);
                snakeLinks.add(0, l); // insert the new link
                snakeNodes.add(0, next_head);
                this.isMoveComplete = false;
                ++cur_node_index;
            } else {
                System.out.println("Head waiting.");
                if (cur_head.mergingNode.flag == -1) {
                    // another snake replaced, free the waiting snake
                    for (Node n : snakeNodes) {
                        n.isWaiting = false;
                    }
                    return;
                }
                if (cur_head.mergingNode.isTail) {
                    // merge
                    merge(snake_map.get(cur_head.mergingNode.flag), this);
                }
                return;
            }
        }

        private void moveBody(int cur_index) {
            snakeNodes.get(cur_index).setColor(Color.RED);
            snakeNodes.get(cur_index).setSize(SNAKE_NODE_SIZE);
            snakeNodes.get(cur_index + 1).setColor(Color.ORANGE);
            snakeNodes.get(cur_index + 1).setSize(Node.DEFAULT_SIZE);
            snakeLinks.get(cur_index - 1).setColor(Color.RED);
            if (cur_index < snakeNodes.size() - 2) {
                snakeLinks.get(cur_index + 1).setColor(Color.ORANGE);
                ++cur_node_index;
            } else {
                // tail move
                snakeNodes.get(cur_index).isTail = true;
                snakeLinks.get(cur_index).setColor(Link.DEFAULT_COLOR);
                snakeLinks.get(cur_index).setWidth(Link.DEFAULT_WIDTH);
                snakeNodes.get(snakeNodes.size() - 1).setSize(Node.DEFAULT_SIZE);
                snakeNodes.get(snakeNodes.size() - 1).flag = -1;
                snakeNodes.get(snakeNodes.size() - 1).isTail = false;
                snakeNodes.remove(snakeNodes.size() - 1);
                snakeLinks.remove(snakeLinks.size() - 1);
                cur_node_index = 0;
                isMoveComplete = true;
            }
        }

        private void resetNode(Node n) {
            n.isWaiting = false;
            n.isTail = false;
            n.mergingNode = null;
            n.flag = -1;
            n.setColor(Color.ORANGE);
            n.setSize(Node.DEFAULT_SIZE);
        }

        private void resetLink(Link l) {
            l.setWidth(Link.DEFAULT_WIDTH);
            l.setColor(Link.DEFAULT_COLOR);
        }
    }

    private class TriangleGridGenerator {
        //private class TriangleGridGenerator implements TopologyGeneratorFactory.Generator {
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
                    boolean isOddRow = i % 2 == 1 ? true : false;
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
                boolean isOddRow = i % 2 == 1 ? true : false;
                result[i] = new Node[xOrder];
                for (int j = 0; j < xOrder; j++) {
                    Node n = gf.getNodeClass().getConstructor().newInstance();
                    if (isOddRow) {
                        n.setLocation(x0 + j * xStep + xStep / 2, y0 + i * yStep);
                    } else {
                        n.setLocation(x0 + j * xStep, y0 + i * yStep);
                    }
                    n.setCommunicationRange(0);
                    n.setColor(Color.ORANGE);
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
        generateTriangleGrid(20, 15);
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
                --i; // rebuild the snake
            } else {
                System.out.println("Snake " + i + " len " + snakeSize);
                snake_map.put(i, s);
            }
        }

        if (retry == maxTry) {
            return;
        }

        setClockModel(new UtilClock(getClockManager()).getClass());
        setClockSpeed(100);
        start();
        isInitialize = true;
    }

    @Override
    public void onClock() {

        for (Iterator<Map.Entry<Integer, Snake>> it = snake_map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Snake> entry = it.next();
            Snake s = entry.getValue();
            //if (s.isMerged) continue;
            System.out.println("Snake " + s.num + " key " + entry.getKey() + " len " + s.snakeNodes.size());
            if (s.isMoveComplete) {
                s.moveHead(s.snakeNodes.get(0));
            } else {
                s.moveBody(s.cur_node_index);
            }

            if (s.isMerged) {
                it.remove();
            }
        }
    }

    public void merge(Snake s1, Snake s2) {
        Node s1_tail = s1.snakeNodes.get(s1.snakeNodes.size() - 1);
        Node s2_head = s2.snakeNodes.get(0);
        s1_tail.isTail = false;
        Link l = s1_tail.getCommonLinkWith(s2_head);
        l.setColor(Color.RED);
        l.setWidth(SNAKE_LINK_WIDTH);
        s1.snakeLinks.add(l);
        s2_head.setColor(Color.RED);

        for (Node n : s2.snakeNodes) {
            n.flag = s1.num;
            n.isWaiting = s1_tail.isWaiting;
        }

        s1.snakeNodes.addAll(s2.snakeNodes);
        s1.snakeLinks.addAll(s2.snakeLinks);
        s2.isMerged = true;
    }
}
