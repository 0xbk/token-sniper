import "./IERC20.sol";

abstract contract SomeToken is IERC20  {
    uint256 public _maxTxAmount;
    address public uniswapV2Pair;
    uint256 public  _liquidityFee;
    uint256 public  _taxFee;

    // uint256 public sellLimit;
    // bool public tradingEnabled;
    // function getSellLockTimeInSeconds() public virtual view returns(uint256);
}
